package config;

import lombok.Getter;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LoadConfigFile {


    private static LoadConfigFile instanceLoadConfig;
    private int intervalProcess;
    private String jsonFile;
    private int snapshotInterval;
    private List<String> snapshotTimes;



    private LoadConfigFile(int intervalProcess, String jsonFile, int snapshotInterval,List<String> snapshotTimes ) {
        this.intervalProcess = intervalProcess;
        this.snapshotTimes = snapshotTimes;
        this.snapshotInterval = snapshotInterval;
        this.jsonFile = jsonFile;
    }

    /**
     * Ugledao sam se na sledeci izvor prilikom kreiranja funkcije za citanje iz config fajla.
     * Izvor: https://www.baeldung.com/java-properties
     * */
    public static LoadConfigFile readConfigFile() {
        if(instanceLoadConfig != null) return instanceLoadConfig;
        Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream("src/config.properties")){

            prop.load(fis);
            int intervalProcess = Integer.parseInt(prop.getProperty("monitor.interval", "3000"));
            String jsonFile = prop.getProperty("mapping.file", "process_info.json");
            int snapshotInterval = Integer.parseInt(prop.getProperty("snapshot.interval", "60"));


            List<String> snapshotFixedTimes = new ArrayList<>();
            for (String key : prop.stringPropertyNames()) {
                if (key.startsWith("snapshot.fixed_time_")) {
                    snapshotFixedTimes.add(prop.getProperty(key));
                }
            }

            instanceLoadConfig = new LoadConfigFile(intervalProcess, jsonFile, snapshotInterval, snapshotFixedTimes);

            return instanceLoadConfig;

        }catch(Exception e){
            System.out.println("READCONFIG - NIJE USPEO DA PRONADJE FAJL");
            e.printStackTrace();
        }

        return null;
    }

    public int getIntervalProcess() {
        return intervalProcess;
    }

    public List<String> getSnapshotTimes() {
        return snapshotTimes;
    }

    public int getSnapshotInterval() {
        return snapshotInterval;
    }

    public String getJsonFile() {
        return jsonFile;
    }
}
