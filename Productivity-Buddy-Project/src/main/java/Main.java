import config.LoadConfigFile;
import javafx.application.Application;
import model.MyProcess;
import view.GuiApplication;

import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) {
        Application.launch(GuiApplication.class, args);
    }
}
