package webserver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.logging.*;

public class WebServerUI extends Application {
    private WebServer webServer;
    private TextField filePathField;
    private TextField logsPathField;
    private TextField portField;
    private final Preferences preferences = Preferences.userNodeForPackage(WebServerUI.class);
    private TextArea directoryHistoryListView;
    private ObservableList<String> directoryHistory;
    private Logger logger;
    private static final String LOG_DIRECTORY = "accesses";
    private static final String LOG_FILE_NAME = "app.log";
    private Button startButton;
    private Button stopButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Web Server Control");

        // Membuat field untuk input file path dan ada tombol browsenya, defaultnya adalah C:\\logwebserver
        Label pathLabel = new Label("File Path:");
        filePathField = new TextField(preferences.get("filePath", "C:\\logwebserver"));
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFilePath(primaryStage));
        HBox filePathBox = new HBox(5, pathLabel, filePathField, browseButton);
        filePathBox.setAlignment(Pos.CENTER_LEFT);

        // Membuat field untuk logs path dan ada tombol browsenya juga, defaultnya adalah C:\\logwebserver
        Label logsPathLabel = new Label("Logs Path:");
        logsPathField = new TextField(preferences.get("logsPath", "C:\\logwebserver"));
        Button logsBrowseButton = new Button("Browse");
        logsBrowseButton.setOnAction(e -> browseLogsPath(primaryStage));
        HBox logsPathBox = new HBox(5, logsPathLabel, logsPathField, logsBrowseButton);
        logsPathBox.setAlignment(Pos.CENTER_LEFT);

        // Membuat field yang dapat diisi dengan port yang diinginkan. port defaultnya adalah 8080
        Label portLabel = new Label("Port:");
        portField = new TextField(preferences.get("port", "8080"));
        VBox portBox = new VBox(5, portLabel, portField);

        // Membuat tombol start
        startButton = new Button("Start");
        startButton.setOnAction(e -> startWebServer()); //"Start" dan "Stop" diaktifkan dan dinonaktifkan sesuai dengan status server.

        // Membuat tombol stop
        stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopWebServer()); //"Start" dan "Stop" diaktifkan dan dinonaktifkan sesuai dengan status server. //Saat server dimulai, tombol "Start" dinonaktifkan dan tombol "Stop" diaktifkan.
        stopButton.setDisable(true); //untuk membuat tombol stop disable saat aplikasi pertamakali dijalankan
        //System.exit(0); //dihapus untuk memastikan aplikasi tidak tertutup saat tombol "Stop" ditekan. 
        
        
        // Membuat riwayat direktori
        directoryHistory = FXCollections.observableArrayList();
        directoryHistoryListView = new TextArea();
        directoryHistoryListView.setPrefHeight(100);
        Label historyLabel = new Label("Accessed Directories:");

        // Menempatkan tombol start dan stop di GUInya
        HBox buttonBox = new HBox(10, startButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);

        // Tampilan GUInya
        VBox layout = new VBox(10, filePathBox, logsPathBox, portBox, buttonBox, historyLabel, directoryHistoryListView);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);

        // Membuat guinya dengan ukuran yang sesuai
        Scene scene = new Scene(layout, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Setup logger
        setupLogger();
        readLogAndUpdateTextArea();
    }

    // Metode untuk browse file path
    private void browseFilePath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select File Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            filePathField.setText(selectedDirectory.getAbsolutePath());
            updateDirectoryHistory(selectedDirectory.getAbsolutePath());
        }
    }

    // Metode untuk browse logs path
    private void browseLogsPath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Logs Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            logsPathField.setText(selectedDirectory.getAbsolutePath());
            updateDirectoryHistory(selectedDirectory.getAbsolutePath());
        }
    }

    // Metode untuk memulai web server
    private void startWebServer() {
        String filePath = filePathField.getText();
        String logsPath = logsPathField.getText();
        int port = Integer.parseInt(portField.getText());

        preferences.put("filePath", filePath);
        preferences.put("logsPath", logsPath);
        preferences.put("port", String.valueOf(port));

        if (webServer == null || !webServer.isAlive()) {
            webServer = new WebServer(filePath, logsPath, port);
            webServer.start();
            directoryHistoryListView.appendText("Server started on port: " + port + "\n");
            logger.info("Server started at port: " + port);
            readLogAndUpdateTextArea();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } else {
            System.out.println("Server is already running.");
        }
    }

    // Metode untuk menghentikan web server
    private void stopWebServer() {
        if (webServer != null && webServer.isAlive()) {
            webServer.stopServer();
            logger.info("Server stopped.");
            directoryHistoryListView.appendText("Server stopped\n");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        }
    }

    // Metode untuk memperbarui riwayat direktori
    private void updateDirectoryHistory(String directory) {
        if (!directoryHistory.contains(directory)) {
            directoryHistory.add(directory);
        }
    }

    // Setup logger
    private void setupLogger() {
        try {
            Files.createDirectories(Paths.get(LOG_DIRECTORY));
            String logFilePath = LOG_DIRECTORY + File.separator + LOG_FILE_NAME;

            Handler fileHandler = new FileHandler(logFilePath, true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            logger = Logger.getLogger(WebServerUI.class.getName());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLogAndUpdateTextArea() {
        try {
            String logFilePath = LOG_DIRECTORY + File.separator + LOG_FILE_NAME;
            Path logPath = Paths.get(logFilePath);
            List<String> lines = Files.readAllLines(logPath);
            StringBuilder logContent = new StringBuilder();
            for (String line : lines) {
                logContent.append(line).append("\n");
            }
            // Memperbarui logTextArea di thread JavaFX Application
            Platform.runLater(() -> {
                directoryHistoryListView.setText(logContent.toString());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}