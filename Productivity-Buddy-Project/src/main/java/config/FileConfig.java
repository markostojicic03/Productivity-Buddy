package config;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class FileConfig {
    private ConcurrentHashMap<String, String> specifyCategory; // key mi je name od procesa, a value category


    public FileConfig() {
        this.specifyCategory = new ConcurrentHashMap<>();
    }


    /**
     * Izvor za citanje json fajla pomocu gson biblioteke: link ->
     * https://github.com/google/gson/blob/main/UserGuide.md#collections-examples
     * */
    public ConcurrentHashMap<String, String> initialScanProcessJsonFile(){
        try (FileReader reader = new FileReader("src/process_info.json")) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<ConcurrentHashMap<String, String>>(){}.getType();
            specifyCategory = gson.fromJson(reader, mapType);
        } catch (Exception e) {
            System.out.println("FILE CONFIG - NIJE PRONADJEN PROCESS_INFO FAJL!");
        }
        return specifyCategory;
    }



}
