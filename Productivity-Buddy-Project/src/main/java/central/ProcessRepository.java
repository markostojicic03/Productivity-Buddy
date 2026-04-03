package central;

import analytic.CategoryAnalytic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.InitialJsonLoading;
import config.LoadConfigFile;
import config.WatchJson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.Getter;
import model.Category;
import model.JsonListProcessDTO;
import model.MyProcess;
import model.MyProcessDto;
import oshi.SystemInfo;
import scanner.ScannerSystem;

import java.io.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private final ReentrantReadWriteLock lockJson = new ReentrantReadWriteLock();;

    public ProcessRepository(LoadConfigFile loadConfigFile, ConcurrentHashMap<Long, MyProcess> data) {
        this.loadConfigFile = loadConfigFile;
        this.data = data;
        this.categoryAnalytic = new CategoryAnalytic(data);
        this.initialJsonLoading = new InitialJsonLoading(loadConfigFile);
        this.initialCategories = initialJsonLoading.initialScanProcessJsonFile();
    }

    public void runProgramThreads(){




        /* POCETAK - Executor za posmatranje json fajla  */
        WatchJson watchJson = new WatchJson(initialCategories, loadConfigFile, lockJson);
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
                            if (myProcess == null) {
                                continue;
                            }
                            System.out.println(myProcess.getName() + " ;TimeActive: " + myProcess.getTimeActive() + " Category: " + myProcess.getCategory().name() + " CPU USAGE: " + myProcess.getUsageCpuPercent() + " RAM USAGE: "+ myProcess.getUsageRamPercent());
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
                File file = new File(nameForSnap());
                boolean flagForWritingTitle = !file.exists();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                    if (flagForWritingTitle) {
                        bw.write("timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name\n");
                    }
                    bw.write(writingText);
                } catch (IOException e) {
                    // error handling
                }
            });

        }, loadConfigFile.getSnapshotInterval(), loadConfigFile.getSnapshotInterval(), TimeUnit.SECONDS);
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
                    File file = new File(nameForSnap());
                    boolean flagForWritingTitle = !file.exists();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                        if (flagForWritingTitle) {
                            bw.write("timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name\n");
                        }
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

            this.categoryAnalytic.refreshTimeInProcessInfo(data, initialCategories);
            jsonWrite(new File(loadConfigFile.getJsonFile()));

        });
    }
    private void jsonWrite(java.io.File targetFile) {
        JsonListProcessDTO container = new JsonListProcessDTO();
        container.setProcesses(new ArrayList<>(initialCategories.values()));

        Gson gsonParsing = new GsonBuilder().setPrettyPrinting().create();
        lockJson.writeLock().lock();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile))) {
            gsonParsing.toJson(container, bw);
        } catch (IOException error) {
            System.err.println("GRESKA PRILIKOM UPISIVANJA U JSON!!! " + error.getMessage());
        }finally {
            lockJson.writeLock().unlock();
        }
    }


    public void saveToFileAsync(java.io.File targetFile) {
        executorJsonChange.submit(() -> {
            this.categoryAnalytic.refreshTimeInProcessInfo(data, initialCategories);
            jsonWrite(targetFile);

            Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION, "System: Data exported successfully!").show();
            });
        });
    }


    public void saveStateSynchronouslyOnShutdown() {
        this.categoryAnalytic.refreshTimeInProcessInfo(data, initialCategories);
        jsonWrite(new File(loadConfigFile.getJsonFile()));
    }


    public void loadChosenFileFromGui(java.io.File sourceFile) {
        executorJsonChange.submit(() -> {
            try (FileReader reader = new FileReader(sourceFile)) {
                Gson gson = new com.google.gson.Gson();
                JsonListProcessDTO listDto = gson.fromJson(reader, JsonListProcessDTO.class);

                if (listDto != null && listDto.getProcesses() != null) {
                    for (MyProcessDto loadedDto : listDto.getProcesses()) {
                        setAttributesForLoad(loadedDto);
                    }

                    makeJsonChange();

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Thank you, Configuration loaded successfully :)");
                        alert.show();
                    });
                }
            } catch (Exception e) {
                System.err.println("GRESKAA pri citanju iz fajla: " + e.getMessage());
            }
        });
    }
    private void setAttributesForLoad(MyProcessDto loadedDto){
        String procName = loadedDto.getOriginalName();
        initialCategories.put(procName, loadedDto);
        for (MyProcess myProcessIter : data.values()) {
            if (myProcessIter.getName().equals(procName)) {
                myProcessIter.setFreezing(loadedDto.isTrackingFreezed());
                myProcessIter.setAliasName(loadedDto.getAliasName());
                try {
                    myProcessIter.setCategory(Category.valueOf(loadedDto.getCategory().toUpperCase()));
                } catch (Exception e) {
                    myProcessIter.setCategory(Category.UNCATEGORIZED);
                }
                myProcessIter.setTimeActive(loadedDto.getTotalTimeSeconds());
            }
        }
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
    private String nameForSnap() {
        // godina_mesec_dan_sat_minut_sekunda_stotinka
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SS");
        String res = "snapshot_" + LocalDateTime.now().format(formatter) + ".csv";
        return res;
    }



}
