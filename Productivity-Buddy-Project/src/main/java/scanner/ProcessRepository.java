package scanner;

import analytic.CategoryAnalytic;
import config.InitialJsonLoading;
import config.LoadConfigFile;
import config.WatchJson;
import model.MyProcess;
import oshi.SystemInfo;

import java.util.concurrent.*;

public class ProcessRepository {
    public static void main(String[] args) {

        LoadConfigFile loadConfigFile = LoadConfigFile.readConfigFile();
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
        ConcurrentHashMap<Long, MyProcess> data = new ConcurrentHashMap<>(); // key -> pid
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

        /* POCETAK - Executor za analystic modul i pisanje u csv fajl  */
        CategoryAnalytic categoryAnalytic = new CategoryAnalytic(data);
        ScheduledExecutorService schedulerCsvAnalytic = Executors.newScheduledThreadPool(1);
        ExecutorService executorCsvAnalytic = Executors.newSingleThreadExecutor();
        schedulerCsvAnalytic.scheduleWithFixedDelay(() -> {
            categoryAnalytic.sumTimeCategories();
            // ovde cu da napravim string koji ce da predstavlja red u csv fajlu
            executorCsvAnalytic.submit(() -> {

            });


        }, 1, loadConfigFile.getSnapshotInterval(), TimeUnit.SECONDS);




        /* KRAJ - Executor za analystic modul i pisanje u csv fajl  */





    }

}
