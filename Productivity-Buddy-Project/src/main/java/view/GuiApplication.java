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
import javafx.geometry.Pos;
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

import javax.swing.*;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class GuiApplication extends Application {

    /** Ovde inicijalizujem osnovne box-ove */
    private final BorderPane borderPane = new BorderPane();
    private final VBox vbox = new VBox();
    private final HBox hboxMenu = new HBox();
    private final HBox hboxTableAndPie = new HBox();
    private final TableView<MyProcess> tableView = new TableView<MyProcess>();
    private final PieChart pieChart = new PieChart();
    private final VBox vboxPieAndCategoryDetails = new VBox();

    @Override
    public void start(Stage stage) throws Exception {
        LoadConfigFile loadConfigFile = LoadConfigFile.readConfigFile();
        ConcurrentHashMap<Long, MyProcess> data = new ConcurrentHashMap<>(); // key -> pid
        ProcessRepository processRepository = new ProcessRepository(loadConfigFile, data);
        processRepository.runProgramThreads();

        stage.setTitle("Process Management Application - Main Chart View");



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
        hboxTableAndPie.getChildren().addAll(tableView);
        HBox.setHgrow(tableView, Priority.ALWAYS);



        /** Tabela */
        TableColumn<MyProcess, String> colProcessName = new TableColumn<>("Process Name");
        TableColumn<MyProcess, String> colProcessCategory = new TableColumn<>("Process Category");
        colProcessName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProcessCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProcessName.setMinWidth(250.0);
        colProcessCategory.setMinWidth(250.0);
        tableView.setMinHeight(700.0);
        tableView.setMaxWidth(600.0);
        ObservableList<MyProcess> fxTableList = FXCollections.observableArrayList();
        tableView.setItems(fxTableList);


        /** Desna strana (od tabele pa na dalje) */
        PieChart.Data chartWorkPart = new PieChart.Data("WORK", 0);
        PieChart.Data chartFunPart = new PieChart.Data("FUN", 0);
        PieChart.Data chartOtherPart = new PieChart.Data("OTHER", 0);
        pieChart.getData().addAll(chartWorkPart, chartFunPart, chartOtherPart);


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


        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // newSelection je zapravo tvoj MyProcess objekat na koji je korisnik kliknuo!
                // Ovde pozivamo logiku za menjanje desne strane ekrana
                hboxTableAndPie.getChildren().remove(vboxPieAndCategoryDetails);
                VBox vboxProcessDetails = vboxProcessDetailsView(newSelection);

                // 3. Ubacujemo novi desni deo pored tabele
                hboxTableAndPie.getChildren().setAll(tableView, vboxProcessDetails);

            }
        });

        btnWorkDetails.setOnAction(e -> {
            VBox categoryView = vboxCategoryDetailsView("WORK", hboxTableAndPie, vboxPieAndCategoryDetails);
            hboxTableAndPie.getChildren().setAll(tableView, categoryView);
        });

        btnFunDetails.setOnAction(e -> {
            VBox categoryView = vboxCategoryDetailsView("FUN", hboxTableAndPie, vboxPieAndCategoryDetails);
            hboxTableAndPie.getChildren().setAll(tableView, categoryView);
        });

        btnOtherDetails.setOnAction(e -> {
            VBox categoryView = vboxCategoryDetailsView("OTHER", hboxTableAndPie, vboxPieAndCategoryDetails);
            hboxTableAndPie.getChildren().setAll(tableView, categoryView);
        });


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


    private VBox vboxProcessDetailsView(MyProcess myProcess){
        VBox vbox = new VBox();
        Button btnBack = new Button("<- Back to Main View");
        btnBack.setOnAction(event -> {
            hboxTableAndPie.getChildren().setAll(tableView, vboxPieAndCategoryDetails);

            tableView.getSelectionModel().clearSelection();
        });

        GridPane gridPaneDetails = new GridPane();
        Label labelProcessName = new Label(myProcess.getName());
        Label labelTotalTime = new Label(secondsToViewFormat(myProcess.getStartingTime()));
        Label labelRamUsagePer = new Label("RAM USAGE "+ myProcess.getUsageRamPercent()+"%  |");
        Label labelCpuUsagePer = new Label("CPU USAGE "+ myProcess.getUsageCpuPercent()+"%  |");
        Label labelRamOrder= new Label("Order");
        Label labelCpuOrder = new Label("Order");
        Button btnKillProcess = new Button("Kill Process");
        Button btnChangeName = new Button("Change Name");
        Button btnFreezeTracking = new Button("Freeze Tracking");
        Button btnChangeCategory = new Button("Change Category");
        btnKillProcess.setPrefSize(120.0, 30.0);
        btnChangeName.setPrefSize(120.0, 30.0);
        btnFreezeTracking.setPrefSize(120.0, 30.0);
        btnChangeCategory.setPrefSize(120.0, 30.0);


        GridPane.setConstraints(labelProcessName, 0, 0, 2, 1, javafx.geometry.HPos.CENTER, javafx.geometry.VPos.CENTER);
        GridPane.setConstraints(labelTotalTime, 0, 1, 2, 1, javafx.geometry.HPos.CENTER, javafx.geometry.VPos.CENTER);

        GridPane.setConstraints(labelRamUsagePer, 0, 2);
        GridPane.setConstraints(labelRamOrder, 1, 2);

        GridPane.setConstraints(labelCpuUsagePer, 0, 3);
        GridPane.setConstraints(labelCpuOrder, 1, 3);


        GridPane.setConstraints(btnKillProcess, 0, 4);
        GridPane.setConstraints(btnChangeName, 1, 4);

        GridPane.setConstraints(btnFreezeTracking, 0, 5);
        GridPane.setConstraints(btnChangeCategory, 1, 5);

        gridPaneDetails.getChildren().addAll(labelProcessName, labelTotalTime,labelRamUsagePer,labelCpuUsagePer,labelRamOrder,labelCpuOrder,btnKillProcess,btnChangeName,btnFreezeTracking, btnChangeCategory );
        gridPaneDetails.setAlignment(Pos.CENTER);
        gridPaneDetails.setVgap(10);
        gridPaneDetails.setHgap(15);
        //vbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        btnBack.setAlignment(Pos.TOP_LEFT);
        vbox.getChildren().addAll(btnBack ,gridPaneDetails);
        return vbox;

    }
    private VBox vboxCategoryDetailsView(String categoryName, HBox hboxTableAndPie, VBox vboxPieAndCategoryDetails) {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(15));
        HBox.setHgrow(vbox, Priority.ALWAYS);

        // 1. Dugme za povratak (identično kao u Process Detail View)
        Button btnBack = new Button("<- Back to Main View");
        btnBack.setOnAction(event -> {
            hboxTableAndPie.getChildren().setAll(hboxTableAndPie.getChildren().get(0), vboxPieAndCategoryDetails);
        });

        // 2. Elementi za kategoriju
        Label titleLabel = new Label("Category Details: " + categoryName);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;"); // Malo da ga ulepšamo

        // Za sada stavljam samo placeholder:
        Label placeholder = new Label("Ovde će ići napredna statistika za " + categoryName);

        VBox contentBox = new VBox(20, titleLabel, placeholder);
        contentBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        vbox.getChildren().addAll(btnBack, contentBox);
        return vbox;
    }



}
