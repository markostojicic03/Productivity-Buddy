package config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Category;
import model.JsonListProcessDTO;
import model.MyProcess;
import model.MyProcessDto;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WatchJson {
    private final ConcurrentHashMap<String, MyProcessDto> startSpecifyCategory;
    private final LoadConfigFile loadConfigFile;
    private final ReentrantReadWriteLock lockJson;

    public WatchJson(ConcurrentHashMap<String, MyProcessDto> startSpecifyCategory, LoadConfigFile loadConfigFile, ReentrantReadWriteLock lockJson ) {
        this.startSpecifyCategory = startSpecifyCategory;
        this.loadConfigFile = loadConfigFile;
        this.lockJson = lockJson;
    }



    public void jsonSpy(ConcurrentHashMap<Long, MyProcess> data) {
        lockJson.readLock().lock();
        try (FileReader reader = new FileReader(loadConfigFile.getJsonFile())) {
            Gson gson = new Gson();
            JsonListProcessDTO dtoListFromJson = gson.fromJson(reader, JsonListProcessDTO.class);
            if (dtoListFromJson == null || dtoListFromJson.getProcesses() == null) {
                return;
            }
            for (MyProcessDto myProcessDtoUpdateVal : dtoListFromJson.getProcesses()) {
                String name = myProcessDtoUpdateVal.getOriginalName();
                MyProcessDto prevDto = startSpecifyCategory.get(name);

                if (prevDto == null || !prevDto.equals(myProcessDtoUpdateVal)) {
                    startSpecifyCategory.put(name, myProcessDtoUpdateVal);
                    setValsForMyProcess(data, name, myProcessDtoUpdateVal);
                }
            }
        } catch (Exception e) {
            System.out.println("JSONSPY ERROR!");
        }finally {
            lockJson.readLock().unlock();
        }
    }
    private void setValsForMyProcess(ConcurrentHashMap<Long, MyProcess> data, String name, MyProcessDto myProcessDtoUpdate) {

        for (MyProcess myProcessIter : data.values()) {
            if (myProcessIter.getName().equals(name)) {
                myProcessIter.setFreezing(myProcessDtoUpdate.isTrackingFreezed());
                myProcessIter.setAliasName(myProcessDtoUpdate.getAliasName());
                try {
                    myProcessIter.setCategory(Category.valueOf(myProcessDtoUpdate.getCategory().toUpperCase()));
                } catch (Exception e) {
                    myProcessIter.setCategory(Category.UNCATEGORIZED);
                }
                myProcessIter.setTimeActive(myProcessDtoUpdate.getTotalTimeSeconds());
            }
        }
    }


}
