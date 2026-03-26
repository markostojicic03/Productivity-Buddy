package model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class MyProcess {

    private String name;
    private long pid;
    private Category category;
    private long timeActive;
    private double usageCpuPercent;
    private double usageRamPercent;
    private long startingTime;

    public MyProcess(String name, long pid, Category category, long timeActive, double usageCpuPercent, long startingTime, double usageRamPercent) {
        this.name = name;
        this.pid = pid;
        this.category = category;
        this.timeActive = timeActive;
        this.usageCpuPercent = usageCpuPercent;
        this.startingTime = startingTime;
        this.usageRamPercent = usageRamPercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyProcess myProcess = (MyProcess) o;
        return pid == myProcess.pid && Objects.equals(name, myProcess.name) && category == myProcess.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pid, category);
    }
}
