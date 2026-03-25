package scanner;

import model.Category;
import model.MyProcess;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ScannerSystem extends RecursiveAction {

    ConcurrentHashMap<Long, MyProcess> data; // ovde hocu da mi pid bude kljuc svakog mog procesa, ma da svakako i unutar procesa cuvam kao atribut pid
    List<ProcessHandle> processes;
    ConcurrentHashMap<String, String> initialCategories;


    public ScannerSystem(ConcurrentHashMap<Long, MyProcess> data, List<ProcessHandle> processes, ConcurrentHashMap<String, String> initialCategories) {
        this.data = data;
        this.processes = processes;
        this.initialCategories = initialCategories;
        //System.out.println("Inside ScannerSystem");
    }

    // PROVERI KAKO DA SETUJEMO USAGE, ZA SADA JE HARDKODOVANO
    private void scanProcesses(List<ProcessHandle> processes){
        for(ProcessHandle process : processes){
            String name = process.info().command().orElse(null);
            if(name == null) continue;
            name = cutString(name);
            long startingTime = process.info().startInstant().orElse(Instant.now()).toEpochMilli();
            String strCat = initialCategories.get(name);
            Category category = (initialCategories.containsKey(name)) ? (Category.valueOf(strCat)) : (Category.OTHER);
            if(data.containsKey(process.pid()) ){
                data.get(process.pid()).setCategory(category);  // OVDE VRSIMO PROMENU AKO JE WATCHER UOCIO NESTO DRUGACIJE
                continue;   /// TU CEMO VEROVATNO VRSITI PROMENU I ZA ALIJAS IME
            }

            MyProcess nw = new MyProcess(name, process.pid(), category, 1, 0.0, startingTime);


            data.put(process.pid(), nw);
        }
    }

    private String cutString(String name){
        int lastInd = name.lastIndexOf('\\');
        return lastInd == -1 ? name : name.substring(lastInd + 1);
    }

    /// IZMENITI DA SE NE DELI UVEK NA PO 2 CUNKA NEGO DA MOZE I NA VISE (PROIZVOLJAN BROJ)


    /**
     * Za definisanje compute funkcije sam se ugledao na nacin na koji smo to radili na vezbama 3, naravno postoji razlika u odnosu na primeru
     * koji smo tada radili jer ja ovde ne vracam vrednost kao u primeru za proste brojeve, vec samo konstantno dodajem elementu u mapu.
     * */
    @Override
    protected void compute() {
        int n = processes.size();
        if(n < 50){
            scanProcesses(processes);
            return;
        }
        int mid = n / 2;
        List<ProcessHandle> leftProcesses = processes.subList(0, mid);
        List<ProcessHandle> rightProcesses = processes.subList(mid, processes.size());

        ScannerSystem left = new ScannerSystem(data, leftProcesses, initialCategories);
        ScannerSystem right = new ScannerSystem(data, rightProcesses, initialCategories);
        left.fork();
        right.compute();
        left.join();

    }



}
