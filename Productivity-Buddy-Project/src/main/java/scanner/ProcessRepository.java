package scanner;

import config.FileConfig;
import config.WatchJson;
import model.MyProcess;

import java.io.File;
import java.util.concurrent.*;

public class ProcessRepository {
    public static void main(String[] args) {
        FileConfig fileConfig = new FileConfig();
        ConcurrentHashMap<String, String> initialCategories = fileConfig.initialScanProcessJsonFile();
        // KRECE EXECUTOR ZA POSMATRANJE JSON-A

        ExecutorService jsonWatch = Executors.newSingleThreadExecutor();
        WatchJson watchJson = new WatchJson(initialCategories);
        jsonWatch.submit(() -> {
            System.out.println("[POSMATRAČ] Započinjem konstantni nadzor...");
            while(true){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                watchJson.jsonSpy();
                //initialCategories.put("chrome.exe", "FUN");
            }
        });

        // ZAVRSAVA SE EXECUTOR ZA POSMATRANJE JSON-A
        ConcurrentHashMap<Long, MyProcess> data = new ConcurrentHashMap<>(); // key -> pid
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(()->{
                forkJoinPool.invoke(new ScannerSystem(data, ProcessHandle.allProcesses().toList(), initialCategories));
                for(long pid : data.keySet()) {
                    boolean processOff = ProcessHandle.of(pid).isEmpty();
                    if(processOff) {
                        data.remove(pid);
                    }
                    else{
                        MyProcess myProcess = data.get(pid);
                        System.out.println(myProcess.getName() + " ;TimeActive: " + myProcess.getTimeActive() + " Category: " + myProcess.getCategory().name()); // TEST
                        myProcess.setTimeActive(myProcess.getTimeActive() + 20);
                    }
                }
        },
        1, 20, TimeUnit.SECONDS);

    }

}
