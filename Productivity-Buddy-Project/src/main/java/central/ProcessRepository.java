package central;

import analytic.CategoryAnalytic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.InitialJsonLoading;
import config.LoadConfigFile;
import config.WatchJson;
import javafx.scene.control.Alert;
import lombok.Getter;
import model.JsonListProcessDTO;
import model.MyProcess;
import model.MyProcessDto;
import oshi.SystemInfo;
import scanner.ScannerSystem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Getter
public class ProcessRepository {

    private final LoadConfigFile loadConfigFile;
    private final ConcurrentHashMap<Long, MyProcess> data;
    private final CategoryAnalytic categoryAnalytic;
    private final InitialJsonLoading initialJsonLoading;
    private final ConcurrentHashMap<String, MyProcessDto> initialCategories;

    private final ExecutorService executorJsonWatch = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService schedulerProcess = Executors.newScheduledThreadPool(1);
    private final ExecutorService executorCsvAnalytic = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService schedulerCsvAnalyticPeriodic = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService schedulerCsvAnalyticBooked = Executors.newScheduledThreadPool(1);
    private final ExecutorService executorJsonChange = Executors.newSingleThreadExecutor();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private volatile boolean flagWatchJson = true;

    public ProcessRepository(LoadConfigFile loadConfigFile, ConcurrentHashMap<Long, MyProcess> data) {
        this.loadConfigFile = loadConfigFile;
        this.data = data;
        this.categoryAnalytic = new CategoryAnalytic(data);
        this.initialJsonLoading = new InitialJsonLoading(loadConfigFile.getJsonFile());
        this.initialCategories = initialJsonLoading.initialScanProcessJsonFile();
    }

    public void runProgramThreads(){




        /* POCETAK - Executor za posmatranje json fajla  */
        WatchJson watchJson = new WatchJson(initialCategories);
        executorJsonWatch.submit(() -> {
            while(flagWatchJson){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (flagWatchJson) {
                    watchJson.jsonSpy(data);
                }
            }
        });
        /* KRAJ - Executor za posmatranje json fajla  */
        /* POCETAK - Executor za posmatranje procesa  */
        SystemInfo systemInfo = new SystemInfo();
        schedulerProcess.scheduleWithFixedDelay(()->{
                    forkJoinPool.invoke(new ScannerSystem(data, ProcessHandle.allProcesses().toList(), initialCategories, systemInfo));
                    for(long pid : data.keySet()) {
                        boolean processOff = ProcessHandle.of(pid).isEmpty();
                        if(processOff) {
                            data.remove(pid);
                        }
                        else{
                            MyProcess myProcess = data.get(pid);
                            System.out.println(myProcess.getName() + " ;TimeActive: " + myProcess.getTimeActive() + " Category: " + myProcess.getCategory().name() + " CPU USAGE: " + myProcess.getUsageCpuPercent() + " RAM USAGE: "+ myProcess.getUsageRamPercent()); // TEST
                            if(!myProcess.getFreezing()){
                                myProcess.setTimeActive(myProcess.getTimeActive() + loadConfigFile.getIntervalProcess() / 1000);
                            }
                        }
                    }
                },
                1, loadConfigFile.getIntervalProcess(), TimeUnit.MILLISECONDS);

        /* KRAJ - Executor za posmatranje procesa  */



        /* POCETAK - Executor za analystic modul i pisanje u csv fajl periodicno  */
        schedulerCsvAnalyticPeriodic.scheduleWithFixedDelay(() -> {
            categoryAnalytic.sumTimeCategories();
            String writingText = categoryAnalytic.makeTextForCsv();
            executorCsvAnalytic.submit(() -> {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("probaPeriodic.csv", true))) {
                    bw.write(writingText);
                } catch (IOException e) {
                    // error handling
                }
            });

        }, 1, loadConfigFile.getSnapshotInterval(), TimeUnit.SECONDS);
        /* KRAJ - Executor za analystic modul i pisanje u csv fajl periodicno  */
        /* POCETAK - Executor za booked csv */
        for(String strTime : loadConfigFile.getSnapshotTimes()){
            LocalTime time = LocalTime.parse(strTime);
            LocalDateTime dateTime = LocalDateTime.of(LocalDate.now(), time);
            if(time.isBefore(LocalTime.now())){
                dateTime = dateTime.plusDays(1);
            }

            schedulerCsvAnalyticBooked.scheduleWithFixedDelay(() -> {
                categoryAnalytic.sumTimeCategories();
                String writingText = categoryAnalytic.makeTextForCsv();
                executorCsvAnalytic.submit(() -> {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter("probaBooked.csv", true))) {
                        bw.write(writingText);
                    } catch (IOException e) {
                        // error handling
                    }
                });
            }, Duration.between(LocalDateTime.now(), dateTime).toSeconds(), 24 * 60 * 60, TimeUnit.SECONDS);

        }
        /* KRAJ - Executor za booked csv*/

    }

    public void destroyProcess(long pid){
        ProcessHandle.of(pid);
        ProcessHandle process = ProcessHandle.of(pid).orElse(null);
        if(process != null){
            process.destroy();
        }
        this.data.remove(pid);
    }

    // 1. IZMENJENA makeJsonChange (sada sinhronizuje vreme pre čuvanja)
    public void makeJsonChange() {
        executorJsonChange.submit(() -> {
            // Prvo prekopiramo živo vreme iz memorije u DTO
            for (MyProcess liveProcess : data.values()) {
                MyProcessDto dto = initialCategories.get(liveProcess.getName());
                if (dto != null) {
                    dto.setTotalTimeSeconds(Math.max(dto.getTotalTimeSeconds(), liveProcess.getTimeActive()));
                }
            }

            try (FileWriter writer = new FileWriter(loadConfigFile.getJsonFile())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonListProcessDTO wrapper = new JsonListProcessDTO();
                wrapper.setProcesses(new ArrayList<>(initialCategories.values()));
                gson.toJson(wrapper, writer);
            } catch (IOException e) {
                System.err.println("GRESKA PRI UPISU U JSON: " + e.getMessage());
            }
        });
    }

    // 2. NOVA METODA: Čuvanje u izabrani fajl (za Save dugme)
    public void saveToFileAsync(java.io.File targetFile) {
        executorJsonChange.submit(() -> {
            for (MyProcess liveProcess : data.values()) {
                MyProcessDto dto = initialCategories.get(liveProcess.getName());
                if (dto != null) {
                    dto.setTotalTimeSeconds(Math.max(dto.getTotalTimeSeconds(), liveProcess.getTimeActive()));
                }
            }

            try (FileWriter writer = new FileWriter(targetFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonListProcessDTO wrapper = new JsonListProcessDTO();
                wrapper.setProcesses(new ArrayList<>(initialCategories.values()));
                gson.toJson(wrapper, writer);

                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "File saved successfully!");
                    alert.show();
                });
            } catch (IOException e) {
                System.err.println("Greska pri cuvanju u fajl: " + e.getMessage());
            }
        });
    }


    public void saveStateSynchronouslyOnShutdown() {
        for (MyProcess liveProcess : data.values()) {
            MyProcessDto dto = initialCategories.get(liveProcess.getName());
            if (dto != null) {
                dto.setTotalTimeSeconds(Math.max(dto.getTotalTimeSeconds(), liveProcess.getTimeActive()));
            }
        }

        try (FileWriter writer = new FileWriter(loadConfigFile.getJsonFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonListProcessDTO wrapper = new JsonListProcessDTO();
            wrapper.setProcesses(new ArrayList<>(initialCategories.values()));
            gson.toJson(wrapper, writer);
        } catch (IOException e) {
            System.err.println("Greška pri čuvanju podataka pred gašenje: " + e.getMessage());
        }
    }


    public void loadFromFileAsync(java.io.File sourceFile) {
        executorJsonChange.submit(() -> {
            try (java.io.FileReader reader = new java.io.FileReader(sourceFile)) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                model.JsonListProcessDTO listDto = gson.fromJson(reader, model.JsonListProcessDTO.class);

                if (listDto != null && listDto.getProcesses() != null) {
                    for (MyProcessDto loadedDto : listDto.getProcesses()) {
                        String name = loadedDto.getOriginalName();

                        // 1. Ažuriramo mapu pravila
                        initialCategories.put(name, loadedDto);

                        // 2. Ažuriramo tabelu
                        for (MyProcess liveProcess : data.values()) {
                            if (liveProcess.getName().equals(name)) {
                                liveProcess.setFreezing(loadedDto.isTrackingFreezed());
                                liveProcess.setAliasName(loadedDto.getAliasName());
                                try {
                                    liveProcess.setCategory(model.Category.valueOf(loadedDto.getCategory().toUpperCase()));
                                } catch (Exception e) {
                                    liveProcess.setCategory(model.Category.UNCATEGORIZED);
                                }
                                liveProcess.setTimeActive(liveProcess.getTimeActive() + loadedDto.getTotalTimeSeconds());
                            }
                        }
                    }

                    // --- 3. KLJUČNO: Odmah prepiši process_info.json! ---
                    // Pošto si osvežio initialCategories, sada to trajno snimi
                    makeJsonChange();
                    // ----------------------------------------------------

                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Configuration loaded successfully!");
                        alert.show();
                    });
                }
            } catch (Exception e) {
                System.err.println("Greska pri citanju iz fajla: " + e.getMessage());
            }
        });
    }

    public void shutdownThreads(){
        flagWatchJson = false;
        executorJsonWatch.shutdown();
        schedulerProcess.shutdown();
        executorCsvAnalytic.shutdown();
        schedulerCsvAnalyticPeriodic.shutdown();
        schedulerCsvAnalyticBooked.shutdown();
        executorJsonChange.shutdown();
        forkJoinPool.shutdown();
    }




}
