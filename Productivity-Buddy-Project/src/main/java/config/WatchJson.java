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

public class WatchJson {
    private final ConcurrentHashMap<String, MyProcessDto> startSpecifyCategory;

    public WatchJson(ConcurrentHashMap<String, MyProcessDto> startSpecifyCategory) {
        this.startSpecifyCategory = startSpecifyCategory;
    }



    public void jsonSpy(ConcurrentHashMap<Long, MyProcess> data) {
        try (FileReader reader = new FileReader("src/process_info.json")) {
            Gson gson = new Gson();
            JsonListProcessDTO listDto = gson.fromJson(reader, JsonListProcessDTO.class);

            for (MyProcessDto myProcessDtoUpdateVal : listDto.getProcesses()) {
                String name = myProcessDtoUpdateVal.getOriginalName();
                MyProcessDto prevDto = startSpecifyCategory.get(name);

                if (prevDto == null || !prevDto.equals(myProcessDtoUpdateVal)) {


                    startSpecifyCategory.put(name, myProcessDtoUpdateVal);

                    for (MyProcess liveProcess : data.values()) {
                        if (liveProcess.getName().equals(name)) {
                            liveProcess.setFreezing(myProcessDtoUpdateVal.isTrackingFreezed());
                            liveProcess.setAliasName(myProcessDtoUpdateVal.getAliasName());
                            try {
                                liveProcess.setCategory(Category.valueOf(myProcessDtoUpdateVal.getCategory().toUpperCase()));
                            } catch (Exception e) {
                                liveProcess.setCategory(Category.UNCATEGORIZED);
                            }
                            liveProcess.setTimeActive(myProcessDtoUpdateVal.getTotalTimeSeconds());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("JSONSPY - ERROR!");
        }
    }


}
