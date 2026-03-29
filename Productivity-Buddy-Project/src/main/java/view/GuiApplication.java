package view;

import analytic.CategoryAnalytic;
import central.ProcessRepository;
import com.sun.jna.platform.win32.Sspi;
import config.LoadConfigFile;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.MyProcess;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class GuiApplication extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        LoadConfigFile loadConfigFile = LoadConfigFile.readConfigFile();
        ConcurrentHashMap<Long, MyProcess> data = new ConcurrentHashMap<>(); // key -> pid
        ProcessRepository processRepository = new ProcessRepository(loadConfigFile, data);
        processRepository.runProgramThreads();

        stage.setTitle("Process Management Application - Main Chart View");

        /** Ovde inicijalizujem osnovne box-ove */
        BorderPane borderPane = new BorderPane();
        VBox vbox = new VBox();
        HBox hboxMenu = new HBox();
        HBox hboxTableAndPie = new HBox();
        TableView<MyProcess> tableView = new TableView<MyProcess>();
        PieChart pieChart = new PieChart();



        /** Menu i osnovne strukture  */
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnShutdown = new Button("Shutdown");
        btnSave.setPrefSize(76.0, 30.0);
        btnLoad.setPrefSize(76.0, 30.0);
        btnShutdown.setPrefSize(76.0, 30.0);


        hboxMenu.getChildren().addAll(btnSave, btnLoad, btnShutdown);
        hboxMenu.setSpacing(5);
        vbox.setSpacing(10);
        hboxTableAndPie.getChildren().addAll(tableView, pieChart);
        HBox.setHgrow(tableView, Priority.ALWAYS);



        /** Tabela */
        TableColumn<MyProcess, String> colProcessName = new TableColumn<>("Process Name");
        TableColumn<MyProcess, String> colProcessCategory = new TableColumn<>("Process Category");
        colProcessName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProcessCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProcessName.setMinWidth(250.0);
        colProcessCategory.setMinWidth(250.0);
        tableView.setMinHeight(700.0);
        ObservableList<MyProcess> fxTableList = FXCollections.observableArrayList();
        tableView.setItems(fxTableList);


        /** Desna strana (od tabele pa na dalje) */
        PieChart.Data chartWorkPart = new PieChart.Data("WORK", 0);
        PieChart.Data chartFunPart = new PieChart.Data("FUN", 0);
        PieChart.Data chartOtherPart = new PieChart.Data("OTHER", 0);
        pieChart.getData().addAll(chartWorkPart, chartFunPart, chartOtherPart);

        VBox vboxPieAndCategoryDetails = new VBox();
        GridPane gridPaneDetailsCategory = new GridPane();
        gridPaneDetailsCategory.setPadding(new Insets(10, 10, 10, 10));
        gridPaneDetailsCategory.setVgap(8);
        gridPaneDetailsCategory.setHgap(10);

        Label labelMoreInfo = new Label("More information: ");
        Label labelWorkCategory = new Label("Work - ");
        Label labelFunCategory = new Label("Fun - ");
        Label labelOtherCategory = new Label("Other - ");
        Label labelWorkTime = new Label("");
        Label labelFunTime = new Label("");
        Label labelOtherTime = new Label("");
        Button btnWorkDetails = new Button("Work details");
        Button btnFunDetails = new Button("Fun details");
        Button btnOtherDetails = new Button("Other details");
        btnWorkDetails.setPrefSize(86.0, 40.0);
        btnFunDetails.setPrefSize(86.0, 40.0);
        btnOtherDetails.setPrefSize(86.0, 40.0);

        GridPane.setConstraints(labelMoreInfo, 5, 0);
        GridPane.setConstraints(labelWorkCategory, 2, 1);
        GridPane.setConstraints(labelFunCategory, 2, 3);
        GridPane.setConstraints(labelOtherCategory, 2, 5);

        GridPane.setConstraints(labelWorkTime, 4, 1);
        GridPane.setConstraints(labelFunTime, 4, 3);
        GridPane.setConstraints(labelOtherTime, 4, 5);

        GridPane.setConstraints(btnWorkDetails, 6, 1);
        GridPane.setConstraints(btnFunDetails, 6, 3);
        GridPane.setConstraints(btnOtherDetails, 6, 5);




        gridPaneDetailsCategory.getChildren().addAll(labelMoreInfo,labelWorkCategory, labelFunCategory, labelOtherCategory, labelWorkTime, labelFunTime, labelOtherTime, btnWorkDetails,btnFunDetails,btnOtherDetails);



        /** DEO ZA OSVEZAVANJE EKRANA  */
        CategoryAnalytic categoryAnalytic = processRepository.getCategoryAnalytic();
        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {

                if (now - lastUpdate >= loadConfigFile.getIntervalProcess() * 1_000_000) {

                    fxTableList.clear();
                    fxTableList.addAll(data.values());

                    lastUpdate = now;


                    categoryAnalytic.sumTimeCategories();

                    chartWorkPart.setPieValue(categoryAnalytic.getWorkTime());
                    chartFunPart.setPieValue(categoryAnalytic.getFunTime());
                    chartOtherPart.setPieValue(categoryAnalytic.getOtherTime());

                    labelWorkTime.setText(secondsToViewFormat(categoryAnalytic.getWorkTime()));
                    labelFunTime.setText(secondsToViewFormat(categoryAnalytic.getFunTime()));
                    labelOtherTime.setText(secondsToViewFormat(categoryAnalytic.getOtherTime()));


                }
            }
        };
        timer.start();
        /** KRAJ DELA ZA OSVEZAVANJE EKRANA */


        vboxPieAndCategoryDetails.getChildren().addAll(pieChart, gridPaneDetailsCategory);
        vbox.getChildren().addAll(hboxMenu, hboxTableAndPie);
        hboxTableAndPie.getChildren().addAll(vboxPieAndCategoryDetails);
        tableView.getColumns().addAll(colProcessName, colProcessCategory);
        borderPane.setCenter(vbox);
        Scene scene = new Scene(borderPane, 1200, 800);
        stage.setScene(scene);
        stage.show();
    }

    private String secondsToViewFormat(long seconds){
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m " + (seconds % 60) + "s ";
    }
}
