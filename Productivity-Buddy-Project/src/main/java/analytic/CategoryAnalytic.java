package analytic;

import lombok.Getter;
import lombok.Setter;
import model.Category;
import model.MyProcess;
import model.MyProcessDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public synchronized void sumTimeCategories(){

        this.workTime = 0;
        this.funTime = 0;
        this.otherTime = 0;


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
            if (process == null) {
                continue;
            }
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

    public String orderRamAndCpu(MyProcess processCheck){
        double processRam = processCheck.getUsageRamPercent();
        double processCpu = processCheck.getUsageCpuPercent();
        int ramOrder = 1;
        int cpuOrder = 1;
        for(MyProcess myProcessIter : data.values()){
            if (myProcessIter.getCategory() == processCheck.getCategory() && myProcessIter.getPid() != processCheck.getPid()){

                if(myProcessIter.getUsageCpuPercent() > processCpu){
                    cpuOrder++;
                }
                if(myProcessIter.getUsageRamPercent() > processRam){
                    ramOrder++;
                }
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

    public void refreshTimeInProcessInfo(ConcurrentHashMap<Long, MyProcess> data , ConcurrentHashMap<String, MyProcessDto> initialCategories) {
        for(long pidEl : data.keySet()){
            if(data.get(pidEl) == null) continue;
            MyProcessDto dto = initialCategories.get(data.get(pidEl).getName());
            if (dto == null) {
                MyProcessDto dtoToWriteInJson = new MyProcessDto();
                dtoToWriteInJson.setOriginalName(data.get(pidEl).getName());
                dtoToWriteInJson.setAliasName(data.get(pidEl).getAliasName());
                dtoToWriteInJson.setCategory(data.get(pidEl).getCategory().name());
                dtoToWriteInJson.setTrackingFreezed(data.get(pidEl).getFreezing());
                dtoToWriteInJson.setTotalTimeSeconds(data.get(pidEl).getTimeActive());
                initialCategories.put(data.get(pidEl).getName(), dtoToWriteInJson);
                continue;
            }
            long time = Math.max(dto.getTotalTimeSeconds(), data.get(pidEl).getTimeActive());
            dto.setTotalTimeSeconds(time);
        }

    }

}
