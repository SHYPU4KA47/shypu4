package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenLibraryFxApp extends Application {

    private final OpenLibraryClient client = new OpenLibraryClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) {
        TextField queryField = new TextField();
        queryField.setPromptText("Введите запрос, например: Толкин");

        Button searchButton = new Button("Искать");
        ListView<String> results = new ListView<>();
        Label status = new Label("Готово");

        searchButton.setOnAction(e -> runSearch(queryField, results, status, searchButton));
        queryField.setOnAction(e -> runSearch(queryField, results, status, searchButton));

        HBox controls = new HBox(8, queryField, searchButton);
        controls.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(results);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(8));

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("OpenLibrary Search");
        stage.show();
    }

    private void runSearch(TextField queryField, ListView<String> results, Label status, Button searchButton) {
        String q = queryField.getText() == null ? "" : queryField.getText().trim();
        if (q.isEmpty()) {
            status.setText("Введите поисковый запрос");
            return;
        }
        searchButton.setDisable(true);
        status.setText("Поиск...");
        results.getItems().clear();

        CompletableFuture
                .supplyAsync(() -> doSearch(q), executor)
                .whenComplete((list, ex) -> Platform.runLater(() -> {
                    searchButton.setDisable(false);
                    if (ex != null) {
                        status.setText("Ошибка: " + ex.getMessage());
                        return;
                    }
                    results.getItems().setAll(list);
                    status.setText(list.isEmpty() ? "Ничего не найдено" : "Найдено: " + list.size());
                }));
    }

    private List<String> doSearch(String query) {
        try {
            return client.searchFormatted(query, 10);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

