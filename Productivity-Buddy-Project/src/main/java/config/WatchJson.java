package config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class WatchJson {
    private final ConcurrentHashMap<String, String> startSpecifyCategory;

    public WatchJson(ConcurrentHashMap<String, String> startSpecifyCategory) {
        this.startSpecifyCategory = startSpecifyCategory;
    }

    /**
     * Izvor koji je koriscen za citanje pomocu gsona naveden u FileConfig fajlu.
     * */
    public void jsonSpy(){
        //ExecutorService executorService = Executors.newCachedThreadPool();
        try (FileReader reader = new FileReader("src/process_info.json")) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<ConcurrentHashMap<String, String>>(){}.getType();
            ConcurrentHashMap<String, String> ongoingCategory = gson.fromJson(reader, mapType);

            for(String nameCat : ongoingCategory.keySet()){
                if(!ongoingCategory.get(nameCat).equals(startSpecifyCategory.get(nameCat))){
                    String nwCategory = ongoingCategory.get(nameCat);
                    startSpecifyCategory.put(nameCat, nwCategory);
                }
            }
            //System.out.println("TEST -> "+"Name: " + "chrome" + ", Category: " + startSpecifyCategory.get("chrome.exe"));

        } catch (Exception e) {
            System.out.println("JSONSPY - NIJE PRONADJEN PROCESS_INFO FAJL!");
        }

    }


}
