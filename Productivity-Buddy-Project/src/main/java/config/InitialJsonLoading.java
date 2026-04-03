package config;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.JsonListProcessDTO;
import model.MyProcessDto;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class InitialJsonLoading {
    private ConcurrentHashMap<String, MyProcessDto> specifyCategory;
    private final LoadConfigFile loadConfigFile;
    public InitialJsonLoading(LoadConfigFile loadConfigFile) {
        this.specifyCategory = new ConcurrentHashMap<>();
        this.loadConfigFile = loadConfigFile;
    }

    /**
     * Preko sledeceg izvora sam se ugledao na koji nacin je potrebno da mapiram json niz u listu objekata.
     * Izvor: https://www.baeldung.com/gson-deserialization-guide
     */
    public ConcurrentHashMap<String, MyProcessDto> initialScanProcessJsonFile(){
        try (FileReader reader = new FileReader(this.loadConfigFile.getJsonFile())) {
            Gson gson = new Gson();
            JsonListProcessDTO listDto = gson.fromJson(reader, JsonListProcessDTO.class);
            for(MyProcessDto myProcessDto : listDto.getProcesses()){
                specifyCategory.put(myProcessDto.getOriginalName(), myProcessDto);
            }
        } catch (Exception e) {
            System.out.println("FILE CONFIG - NIJE PRONADJEN PROCESS_INFO FAJL!");
        }
        return specifyCategory;
    }



}
