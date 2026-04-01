package analytic;

import lombok.Getter;
import lombok.Setter;
import model.Category;
import model.MyProcess;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
public class CategoryAnalytic {

    private ConcurrentHashMap<Long, MyProcess> data;
    private long workTime;
    private long funTime;
    private long otherTime;

    public CategoryAnalytic(ConcurrentHashMap<Long, MyProcess> data) {
        this.data = data;
    }

    public void sumTimeCategories(){

        this.workTime = 0;
        this.funTime = 0;
        this.otherTime = 0;

        // koristimo grupisane liste, gde imamo samo JEDAN Chrome, JEDAN IntelliJ itd.
        for(MyProcess p : getProcessesForCategory(Category.WORK)){
            this.workTime += p.getTimeActive();
        }

        for(MyProcess p : getProcessesForCategory(Category.FUN)){
            this.funTime += p.getTimeActive();
        }

        for(MyProcess p : getProcessesForCategory(Category.OTHER)){
            this.otherTime += p.getTimeActive();
        }
    }


    public String retLinesCsv(MyProcess process){
        return  LocalDateTime.now() +","+ process.getPid() + "," + process.getName() + "," + process.getUsageCpuPercent() + "," + process.getUsageRamPercent() + "," + process.getCategory() + "," + process.getAliasName();
    }


    public String makeTextForCsv(){
        StringBuilder strBuilder = new StringBuilder();
        for(long pid : data.keySet()){
            MyProcess process = data.get(pid);
            strBuilder.append(this.retLinesCsv(process)).append("\n");
        }

        return strBuilder.toString();
    }



    public List<MyProcess> getProcessesForCategory(Category category) {

        Map<String, MyProcess> groupedProcesses = new HashMap<>();


        for (MyProcess p : data.values()) {
            if (p.getCategory().equals(category)) {
                String name = p.getAliasName();

                if (groupedProcesses.containsKey(name)) {

                    MyProcess existing = groupedProcesses.get(name);

                    existing.setTimeActive(Math.max(existing.getTimeActive(), p.getTimeActive()));

                    existing.setUsageCpuPercent(existing.getUsageCpuPercent() + p.getUsageCpuPercent());
                    existing.setUsageRamPercent(existing.getUsageRamPercent() + p.getUsageRamPercent());
                } else {

                    MyProcess copy = new MyProcess(
                            p.getName(), 0, p.getCategory(), p.getTimeActive(),
                            p.getUsageCpuPercent(), 0, p.getUsageRamPercent(),
                            p.getAliasName(), p.getFreezing()
                    );
                    groupedProcesses.put(name, copy);
                }
            }
        }


        List<MyProcess> resultList = new ArrayList<>(groupedProcesses.values());


        resultList.sort((p1, p2) -> Long.compare(p2.getTimeActive(), p1.getTimeActive()));

        return resultList;
    }

    public String orderRamAndCpu(MyProcess process){
        List<MyProcess> allInCategory = getProcessesForCategory(process.getCategory());
        double processRam = process.getUsageRamPercent();
        double processCpu = process.getUsageCpuPercent();
        int ramOrder = 1;
        int cpuOrder = 1;
        for(MyProcess p : allInCategory){
            if(p.getUsageCpuPercent() > processCpu){
                cpuOrder++;
            }
            if(p.getUsageRamPercent() > processRam){
                ramOrder++;
            }
        }

        return ramOrder+"th on RAM usage-"+cpuOrder+"th ON CPU usage";
    }




    public List<MyProcess> getTopTen(Category category) {
        List<MyProcess> allInCategory = getProcessesForCategory(category);

        if (allInCategory.size() <= 10) {
            return allInCategory;
        } else {
            return allInCategory.subList(0, 10);
        }

    }

}
