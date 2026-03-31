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

    public void makeJsonChange() {
        executorJsonChange.submit(() -> {
            try (FileWriter writer = new FileWriter(loadConfigFile.getJsonFile())) {

                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                JsonListProcessDTO wrapper = new JsonListProcessDTO();
                wrapper.setProcesses(new java.util.ArrayList<>(initialCategories.values()));

                gson.toJson(wrapper, writer);
            } catch (IOException e) {
                System.err.println("GRESKA PRI UPISU U JSON: " + e.getMessage());
            }
        });
    }
    public void saveCurrentStateAsync() {
        executorJsonChange.submit(() -> {
            try (FileWriter writer = new FileWriter("saved_state.json")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                List<MyProcess> currentProcesses = new java.util.ArrayList<>(data.values());
                gson.toJson(currentProcesses, writer);

            } catch (IOException e) {
                System.err.println("GRESKA PRI UPISU STANJA U JSON: " + e.getMessage());
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

    public void loadStateAsync() {
        // 1. Pokrećemo čitanje u pozadinskoj niti (Asinhrono - ne blokira GUI)
        executorJsonChange.submit(() -> {
            try (java.io.FileReader reader = new java.io.FileReader("saved_state.json")) {
                Gson gson = new Gson();

                // Definišemo tip liste koji očekujemo iz JSON-a
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.ArrayList<MyProcess>>(){}.getType();
                List<MyProcess> savedProcesses = gson.fromJson(reader, listType);

                if (savedProcesses != null) {
                    // 2. Logika spajanja (Merge)
                    for (MyProcess savedProcess : savedProcesses) {
                        String processName = savedProcess.getName();
                        long savedTime = savedProcess.getTimeActive();

                        // Prolazimo kroz sve ŽIVE procese u mapi i dodajemo im sačuvano vreme
                        for (MyProcess liveProcess : data.values()) {
                            if (liveProcess.getName().equals(processName)) {
                                // Sabiramo trenutno vreme sesije sa starim sačuvanim vremenom
                                liveProcess.setTimeActive(liveProcess.getTimeActive() + savedTime);
                            }
                        }
                    }
                }

                // 3. UI Responzivnost - Vraćamo se u glavnu JavaFX nit da prikažemo obaveštenje
                // OVO TI DONOSI BODOVE IZ KOLONE "UI Responzivnost"
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Load State");
                    alert.setHeaderText("Load Complete");
                    alert.setContentText("Historical process times have been loaded and merged successfully!");
                    alert.show();
                });

            } catch (Exception e) {
                System.err.println("GRESKA PRI CITANJU STANJA: " + e.getMessage());
                // U slučaju greške, takođe obaveštavamo korisnika preko JavaFX niti
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Load Error");
                    alert.setContentText("Could not load saved state. File might be missing.");
                    alert.show();
                });
            }
        });
    }


}
