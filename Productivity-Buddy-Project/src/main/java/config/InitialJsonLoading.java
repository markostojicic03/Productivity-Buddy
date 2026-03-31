package config;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.JsonListProcessDTO;
import model.MyProcessDto;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class InitialJsonLoading {
    private ConcurrentHashMap<String, MyProcessDto> specifyCategory; // key mi je name od procesa, a value category
    private String jsonPath;

    public InitialJsonLoading(String jsonPath) {
        this.specifyCategory = new ConcurrentHashMap<>();
        this.jsonPath = jsonPath;
    }


    public ConcurrentHashMap<String, MyProcessDto> initialScanProcessJsonFile(){
        try (FileReader reader = new FileReader("src/process_info.json")) {
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
