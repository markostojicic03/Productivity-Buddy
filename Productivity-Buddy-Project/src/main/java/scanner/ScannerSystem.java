package scanner;

import model.MyProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ScannerSystem extends RecursiveAction {

    ConcurrentHashMap<Long, MyProcess> data; // ovde hocu da mi pid bude kljuc svakog mog procesa, ma da svakako i unutar procesa cuvam kao atribut pid
    List<ProcessHandle> processes;


    public ScannerSystem(ConcurrentHashMap<Long, MyProcess> data, List<ProcessHandle> processes) {
        this.data = data;
        this.processes = processes;
        //System.out.println("Inside ScannerSystem");
    }


    private void scanProcesses(List<ProcessHandle> processes){
        System.out.println("Inside scanProcess");
    }


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

        ScannerSystem left = new ScannerSystem(data, leftProcesses);
        ScannerSystem right = new ScannerSystem(data, rightProcesses);
        left.fork();
        right.compute();
        left.join();

    }



}
