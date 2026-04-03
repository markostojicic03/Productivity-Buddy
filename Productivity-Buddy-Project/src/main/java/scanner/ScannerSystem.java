package scanner;

import model.Category;
import model.MyProcess;
import model.MyProcessDto;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ScannerSystem extends RecursiveAction {

    ConcurrentHashMap<Long, MyProcess> data;
    List<ProcessHandle> processes;
    ConcurrentHashMap<String, MyProcessDto> initialCategories;
    SystemInfo systemInfo;


    public ScannerSystem(ConcurrentHashMap<Long, MyProcess> data, List<ProcessHandle> processes, ConcurrentHashMap<String, MyProcessDto> initialCategories, SystemInfo systemInfo) {
        this.data = data;
        this.processes = processes;
        this.initialCategories = initialCategories;
        this.systemInfo = systemInfo;
    }

    private void scanProcesses(List<ProcessHandle> processes){
        long wholeRamMemory = systemInfo.getHardware().getMemory().getTotal();
        int numOfLogicalUnits = systemInfo.getHardware().getProcessor().getLogicalProcessorCount();
        for(ProcessHandle process : processes){
            String name = process.info().command().orElse(null);
            if(name == null) continue;
            name = cutString(name);
            long startingTime = process.info().startInstant().orElse(Instant.now()).toEpochMilli();


            Category category = Category.UNCATEGORIZED;
            String alias = name;
            boolean isFreezed = false;

            if (initialCategories.containsKey(name)) {
                MyProcessDto dto = initialCategories.get(name);
                try {
                    category = Category.valueOf(dto.getCategory().toUpperCase());
                } catch (IllegalArgumentException e) {
                    category = Category.OTHER;
                }
                alias = dto.getAliasName();
                isFreezed = dto.isTrackingFreezed();
            }


            double numCpu = 0;
            double numRam = 0;

            try {
                OSProcess processFromOshi = systemInfo.getOperatingSystem().getProcess((int) process.pid());

                if(processFromOshi != null){
                    numCpu = (processFromOshi.getProcessCpuLoadCumulative() * 100.0) / numOfLogicalUnits;
                    numRam = (processFromOshi.getResidentSetSize() * 100.0) / wholeRamMemory;
                }
            } catch (Exception e) {
            }

            if(data.containsKey(process.pid()) ){
                data.get(process.pid()).setUsageCpuPercent(numCpu);
                data.get(process.pid()).setUsageRamPercent(numRam);
                continue;
            }
            MyProcessDto myProcessDto = initialCategories.get(name);
            long timeActivePlaceholder = 1;
            if(myProcessDto != null) timeActivePlaceholder = myProcessDto.getTotalTimeSeconds();
            MyProcess nw = new MyProcess(name, process.pid(), category, timeActivePlaceholder, numCpu, startingTime, numRam, alias, isFreezed);


            data.put(process.pid(), nw);
        }
    }

    private String cutString(String name){
        int lastInd = name.lastIndexOf('\\');
        return lastInd == -1 ? name : name.substring(lastInd + 1);
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

        ScannerSystem left = new ScannerSystem(data, leftProcesses, initialCategories, systemInfo);
        ScannerSystem right = new ScannerSystem(data, rightProcesses, initialCategories, systemInfo);
        left.fork();
        right.compute();
        left.join();

    }



}
