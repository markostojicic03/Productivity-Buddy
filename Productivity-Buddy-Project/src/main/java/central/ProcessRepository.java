package central;

import analytic.CategoryAnalytic;
import config.InitialJsonLoading;
import config.LoadConfigFile;
import config.WatchJson;
import lombok.Getter;
import model.MyProcess;
import oshi.SystemInfo;
import scanner.ScannerSystem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.*;

@Getter
public class ProcessRepository {

    private final LoadConfigFile loadConfigFile;
    private final ConcurrentHashMap<Long, MyProcess> data;
    private final CategoryAnalytic categoryAnalytic;

    public ProcessRepository(LoadConfigFile loadConfigFile, ConcurrentHashMap<Long, MyProcess> data) {
        this.loadConfigFile = loadConfigFile;
        this.data = data;
        this.categoryAnalytic = new CategoryAnalytic(data);
    }

    public void runProgramThreads(){

        InitialJsonLoading initialJsonLoading = new InitialJsonLoading(loadConfigFile.getJsonFile());
        ConcurrentHashMap<String, String> initialCategories = initialJsonLoading.initialScanProcessJsonFile();


        /* POCETAK - Executor za posmatranje json fajla  */
        ExecutorService executorJsonWatch = Executors.newSingleThreadExecutor();
        WatchJson watchJson = new WatchJson(initialCategories);
        executorJsonWatch.submit(() -> {
            while(true){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                watchJson.jsonSpy();
            }
        });
        /* KRAJ - Executor za posmatranje json fajla  */
        /* POCETAK - Executor za posmatranje procesa  */
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ScheduledExecutorService schedulerProcess = Executors.newScheduledThreadPool(1);
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
                            myProcess.setTimeActive(myProcess.getTimeActive() + loadConfigFile.getIntervalProcess() / 1000);
                        }
                    }
                },
                1, loadConfigFile.getIntervalProcess(), TimeUnit.MILLISECONDS);

        /* KRAJ - Executor za posmatranje procesa  */



        /* POCETAK - Executor za analystic modul i pisanje u csv fajl periodicno  */
        ScheduledExecutorService schedulerCsvAnalyticPeriodic = Executors.newScheduledThreadPool(1);
        ExecutorService executorCsvAnalytic = Executors.newSingleThreadExecutor();
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
        ScheduledExecutorService schedulerCsvAnalyticBooked = Executors.newScheduledThreadPool(1);
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

}
