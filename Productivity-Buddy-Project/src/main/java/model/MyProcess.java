package model;


import lombok.Getter;
import lombok.Setter;

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

}
