package model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class MyProcess {
    // za tipove treba staviti AtomicInteger i slicno, ali to promeniti kasnije !!!!

    private String name;
    private long pid;
    private Category category;
    private long timeActive; // koliko dugo mi je aktivan proces
    private double usage; // koliko mi iznosi usage od cpu-a za Details stranicu
    private long startingTime; // kada smo pokrenuli proces

    public MyProcess(String name, long pid, Category category, long timeActive, double usage, long startingTime) {
        this.name = name;
        this.pid = pid;
        this.category = category;
        this.timeActive = timeActive;
        this.usage = usage;
        this.startingTime = startingTime;
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
