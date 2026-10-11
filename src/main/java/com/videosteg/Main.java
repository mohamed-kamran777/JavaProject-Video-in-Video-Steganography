
package com.videosteg;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    private File coverFile;
    private File secretFile;

    private final Label coverLabel =
            new Label("No cover video selected");

    private final Label secretLabel =
            new Label("No secret video selected");

    private final Label statusLabel = new Label("Ready");

    private final Button hideButton = new Button("Hide Video");
    private final Button extractButton = new Button("Extract Video");

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) {

        Button coverButton = new Button("Choose Cover Video");
        Button secretButton = new Button("Choose Secret Video");

        coverButton.setOnAction(event -> {
            File selected = chooseVideo(stage);
            if (selected != null) {
                coverFile = selected;
                coverLabel.setText(selected.getName());
            }
        });

        secretButton.setOnAction(event -> {
            File selected = chooseVideo(stage);
            if (selected != null) {
                secretFile = selected;
                secretLabel.setText(selected.getName());
            }
        });

        hideButton.setOnAction(event -> hide());
        extractButton.setOnAction(event -> extract());

        Label title = new Label("VIDEO-IN-VIDEO STEGANOGRAPHY");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label note = new Label(
                "Videos are automatically resized and converted to 10 FPS.");

        note.setWrapText(true);

        VBox root = new VBox(
                16,
                title,
                coverButton,
                coverLabel,
                secretButton,
                secretLabel,
                note,
                hideButton,
                extractButton,
                statusLabel
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("""
                -fx-font-family: 'Arial';
                -fx-font-size: 14px;
                -fx-background-color: #f4f7fb;
                """);

        hideButton.setPrefWidth(220);
        extractButton.setPrefWidth(220);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(540);

        stage.setTitle("Video Steganography");
        stage.setScene(new Scene(root, 650, 480));
        stage.show();
    }

    private File chooseVideo(Stage stage) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Video");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Video files",
                        "*.mp4", "*.mkv", "*.avi", "*.mov",
                        "*.webm", "*.m4v"
                )
        );

        return chooser.showOpenDialog(stage);
    }

    private void hide() {

        if (coverFile == null || secretFile == null) {
            statusLabel.setText(
                    "Select both the cover and secret videos first.");
            return;
        }

        if (coverFile.equals(secretFile)) {
            statusLabel.setText(
                    "Choose two different video files.");
            return;
        }

        runTask(
                "Embedding secret video. Please wait...",
                () -> {
                    Files.createDirectories(Path.of("input"));

                    Files.copy(
                            coverFile.toPath(),
                            Path.of("input/cover.mp4"),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    Files.copy(
                            secretFile.toPath(),
                            Path.of("input/secret_small.mp4"),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    VideoSteganographyService.hideVideo();
                },
                "Embedding complete: output/stego.mkv"
        );
    }

    private void extract() {

        runTask(
                "Extracting secret from saved stego video...",
                VideoSteganographyService::extractVideo,
                "Extraction complete: output/recovered_secret.mkv"
        );
    }

    private void runTask(
            String message,
            TaskAction action,
            String successMessage) {

        hideButton.setDisable(true);
        extractButton.setDisable(true);
        statusLabel.setText(message);

        executor.submit(() -> {
            try {
                action.run();

                Platform.runLater(
                        () -> statusLabel.setText(successMessage));

            } catch (Exception exception) {

                String error = exception.getMessage();

                if (error == null || error.isBlank()) {
                    error = exception.getClass().getSimpleName();
                }

                String finalError = error;

                Platform.runLater(
                        () -> statusLabel.setText("Error: " + finalError));

                exception.printStackTrace();

            } finally {
                Platform.runLater(() -> {
                    hideButton.setDisable(false);
                    extractButton.setDisable(false);
                });
            }
        });
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    @FunctionalInterface
    interface TaskAction {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
