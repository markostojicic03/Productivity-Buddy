package analytic;

import lombok.Getter;
import lombok.Setter;
import model.MyProcess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
        // ovde resetujemo prvo sve vrednosti - RAZMISLITI DA LI OVO TREBA DA OSTANE
        this.workTime = 0;
        this.funTime = 0;
        this.otherTime = 0;

        for(long key: data.keySet()){
            MyProcess process = data.get(key);
            if(process.getCategory().toString().equals("WORK")){
                this.workTime+= process.getTimeActive();
            }
            else if(process.getCategory().toString().equals("FUN")){
                this.funTime+= process.getTimeActive();
            }
            else{
                this.otherTime+= process.getTimeActive();
            }
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
}
