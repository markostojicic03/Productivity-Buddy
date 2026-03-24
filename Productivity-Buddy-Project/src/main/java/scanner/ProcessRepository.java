package scanner;

import model.MyProcess;

import java.util.concurrent.*;

public class ProcessRepository {
    public static void main(String[] args) {
        ConcurrentHashMap<Long, MyProcess> data = new ConcurrentHashMap<>(); // key -> pid
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(()->{
                //System.out.println("Ovde pokrecem fiksirani scheduler probaa");
                forkJoinPool.invoke(new ScannerSystem(data, ProcessHandle.allProcesses().toList()));
                for(long pid : data.keySet()) {
                    boolean processOff = ProcessHandle.of(pid).isEmpty();
                    if(processOff) {
                        data.remove(pid);
                    }
                    else{
                        MyProcess myProcess = data.get(pid);
                        System.out.println(myProcess.getName() + " ;TimeActive: " + myProcess.getTimeActive()); // TEST
                        myProcess.setTimeActive(myProcess.getTimeActive() + 20);
                    }
                }
        },
        1, 20, TimeUnit.SECONDS);

    }

}
