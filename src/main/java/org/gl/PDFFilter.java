package org.gl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

public class PDFFilter extends JFrame {

    private JTextField searchField;
    private JLabel fileLabel;
    private JTextField outputPathField;
    private File selectedFile;

    public PDFFilter() {
        setTitle("PDF Filter");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1));

        JButton fileButton = new JButton("Select PDF File");
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    fileLabel.setText("Selected File: " + selectedFile.getName());
                    outputPathField.setText(new File(selectedFile.getParent(), "filtered_output.pdf").getAbsolutePath());
                }
            }
        });
        panel.add(fileButton);

        fileLabel = new JLabel("No file selected");
        panel.add(fileLabel);

        JLabel searchLabel = new JLabel("Enter search term:");
        panel.add(searchLabel);

        searchField = new JTextField();
        searchField.setToolTipText("Enter the search term");
        panel.add(searchField);

        JLabel outputLabel = new JLabel("Output file path:");
        panel.add(outputLabel);

        outputPathField = new JTextField();
        outputPathField.setToolTipText("Output file path");
        panel.add(outputPathField);

        add(panel, BorderLayout.CENTER);

        JButton processButton = new JButton("Process PDF");
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchTerm = searchField.getText();
                String outputPath = outputPathField.getText();
                if (selectedFile != null && !searchTerm.isEmpty() && !outputPath.isEmpty()) {
                    File outputFile = new File(outputPath);
                    if (outputFile.exists()) {
                        int response = JOptionPane.showConfirmDialog(null, "File already exists. Do you want to replace it?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (response == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                    processPDF(selectedFile, searchTerm, outputPath);
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a file, enter a search term, and specify an output path.");
                }
            }
        });
        add(processButton, BorderLayout.SOUTH);
    }

    private void processPDF(File inputFile, String searchTerm, String outputFilePath) {
        searchTerm = searchTerm.toUpperCase(); // Converti il termine di ricerca in maiuscolo per una corrispondenza senza distinzioni tra maiuscole e minuscole

        try (PDDocument document = PDDocument.load(inputFile)) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            // Definisci la regione di interesse (in questo caso, l'area in alto a sinistra)
            Rectangle rect = new Rectangle(0, 130, 320, 23); // Modifica le coordinate e le dimensioni secondo il layout del tuo PDF
            stripper.addRegion("topLeft", rect);

            List<Integer> pagesToKeep = new ArrayList<>();
            boolean lastHeaderIncluded = false;

            // Itera attraverso tutte le pagine
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                PDPage pdPage = document.getPage(page);
                stripper.extractRegions(pdPage);
                String regionText = stripper.getTextForRegion("topLeft").toUpperCase();
                boolean isHeader = regionText.contains("INDIRIZZO SEDE DI LAVORO");

                if (isHeader && regionText.contains(searchTerm)) {
                    lastHeaderIncluded = true;
                    pagesToKeep.add(page + 1); // PDFBox utilizza 0-based indexing, quindi aggiungiamo 1 per corrispondere alla numerazione delle pagine
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
                JOptionPane.showMessageDialog(null, "No matches found for the search term: " + searchTerm);
                return;
            }

            // Salva le pagine filtrate in un nuovo file PDF
            document.save(outputFilePath);
            JOptionPane.showMessageDialog(null, "Pagine filtrate e salvate nel file: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error processing PDF: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PDFFilter().setVisible(true);
            }
        });
    }
}
