package split;

import java.awt.*;
import java.util.Objects;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFFilterFX extends Application {

    private TextField searchField;
    private TextField headerIndoToSearchFor;
    private Label fileLabel;
    private TextField outputPathField;
    private File selectedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("PDF Filter");

        GridPane grid = new GridPane();
        // set width 100% of the window
        grid.prefWidthProperty().bind(primaryStage.widthProperty());
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(10);
        grid.setHgap(10);

        Button fileButton = new Button("Select PDF File");
        fileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                fileLabel.setText("Selected File: " + selectedFile.getName());
                outputPathField.setText(new File(selectedFile.getParent(), "filtered_output.pdf").getAbsolutePath());
            }
        });
        GridPane.setConstraints(fileButton, 0, 0);

        fileLabel = new Label("No file selected");
        GridPane.setConstraints(fileLabel, 1, 0);

        Label searchLabel = new Label("Enter search text:");
        GridPane.setConstraints(searchLabel, 0, 1);

        searchField = new TextField();
        searchField.setPromptText("");
        GridPane.setConstraints(searchField, 1, 1);

        Label headerInfoToSearchFor = new Label("Enter header information to search for:");
        GridPane.setConstraints(headerInfoToSearchFor, 0, 2);

        headerIndoToSearchFor = new TextField();
        headerIndoToSearchFor.setPromptText("");
        headerIndoToSearchFor.setPrefWidth(300);
        GridPane.setConstraints(headerIndoToSearchFor, 1, 2);

        Label outputLabel = new Label("Output file path:");
        GridPane.setConstraints(outputLabel, 0, 3);

        outputPathField = new TextField();
        outputPathField.setPromptText("Output file path");
        GridPane.setConstraints(outputPathField, 1, 3);

        Button processButton = new Button("Process PDF");
        processButton.setOnAction(e -> {
            String searchTerm = searchField.getText();
            String addressKeyword = headerIndoToSearchFor.getText();
            String outputPath = outputPathField.getText();
            if (selectedFile != null && !searchTerm.isEmpty() && !outputPath.isEmpty()) {
                File outputFile = new File(outputPath);
                if (outputFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("File exists");
                    alert.setHeaderText(null);
                    alert.setContentText("File already exists. Do you want to replace it?");
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                        return;
                    }
                }
                processPDF(selectedFile, searchTerm, addressKeyword, outputPath);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText(null);
                alert.setContentText("Please select a file, enter search and address keywords, and specify an output path.");
                alert.showAndWait();
            }
        });
        GridPane.setConstraints(processButton, 1, 4);

        grid.getChildren().addAll(fileButton, fileLabel, searchLabel, searchField, headerInfoToSearchFor, headerIndoToSearchFor, outputLabel, outputPathField, processButton);

        Scene scene = new Scene(grid, 600, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void processPDF(File inputFile, String searchTerm, String headerSearchKeyRef, String outputFilePath) {
        searchTerm = searchTerm.toUpperCase(); // Converti il termine di ricerca in maiuscolo per una corrispondenza senza distinzioni tra maiuscole e minuscole
        headerSearchKeyRef = Objects.requireNonNullElse (headerSearchKeyRef, "").toUpperCase(); // Converti il keyword dell'indirizzo in maiuscolo per una corrispondenza senza distinzioni tra maiuscole e minuscole

        try (PDDocument document = PDDocument.load(inputFile)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            Rectangle rect = new Rectangle(0, 130, 320, 23); // Definisci la regione di interesse (in questo caso, l'area in alto a sinistra)
            stripper.addRegion("topLeft", rect);

            List<Integer> pagesToKeep = new ArrayList<>();
            boolean lastHeaderIncluded = false;

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                PDPage pdPage = document.getPage(page);
                stripper.extractRegions(pdPage);
                String regionText = stripper.getTextForRegion("topLeft").toUpperCase();
                boolean isHeader = headerSearchKeyRef.isEmpty () || regionText.contains(headerSearchKeyRef);

                if (isHeader && regionText.contains(searchTerm)) {
                    lastHeaderIncluded = true;
                    pagesToKeep.add(page + 1);
                } else if (isHeader) {
                    lastHeaderIncluded = false;
                    document.removePage(page);
                    page--;
                } else {
                    if (lastHeaderIncluded) {
                        pagesToKeep.add(page + 1);
                    } else {
                        document.removePage(page);
                        page--;
                    }
                }
            }

            if (pagesToKeep.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information");
                alert.setHeaderText(null);
                alert.setContentText("No matches found for the search term and address keyword.");
                alert.showAndWait();
                return;
            }

            document.save(outputFilePath);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Filtered pages saved to: " + outputFilePath);
            alert.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error processing PDF: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
