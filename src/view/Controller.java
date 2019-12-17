package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller implements Observer, Initializable {
    private String postPath = "";
    private ObservableList stemmerChoise = FXCollections.observableArrayList();
    @FXML
    private javafx.scene.control.Button broeseB;
    @FXML
    private javafx.scene.control.Button resetB;
    @FXML
    private javafx.scene.control.TextArea txtBrowse;
    @FXML
    private javafx.scene.control.TextArea txtPosting;
    @FXML
    private javafx.scene.control.ComboBox<String> comboStemmer;


    public void BrowserButtonAction(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            txtBrowse.setText(selectedDirectory.getAbsolutePath());
        } else {
            System.out.println("file is not valid");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }


    private void loadData() {
        stemmerChoise.removeAll(stemmerChoise);
        String name_S1 = "with Stemmer";
        String name_S2 = "without Stemer";
        stemmerChoise.addAll(name_S1, name_S2);
        comboStemmer.getItems().addAll(stemmerChoise);
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    @FXML
    private void startIR() throws IOException {
        String stem = comboStemmer.getValue();
        String postPath = txtPosting.getText();
        String corpusPath = txtBrowse.getText();

        if (stem == null) {
            showAlert("please select stemmer option");

        }
        if (postPath == null) {
            showAlert("please enter posting path");

        }
        if (corpusPath == null) {
            showAlert("please enter corpus path");

        }
        if (stem != null && corpusPath != null && postPath != null) {
            File postDir = new File(postPath);
            File corpusdir = new File(corpusPath);
            if (postDir.isDirectory() == false) {
                showAlert("please enter correct posting path");
            }
            if (corpusdir.isDirectory() == false) {
                showAlert("please enter correct corpus path");
            }

            /************************************///////////////////////


            File[] files1 = null;
            File folder = new File(corpusPath);
            //Indexer indexer= new Indexer(true);
            ExecutorService executor = Executors.newFixedThreadPool(5);
            File subFolderTerms = null;
            boolean corpus;
            boolean subFolder1;
            boolean subFolder2;

            boolean stemming = stem.equals("with Stemmer");
            if (stemming) {
                File directory = new File(postPath + "/StemmedCorpus");
                corpus = directory.mkdir();
                subFolderTerms = new File(postPath + "/StemmedCorpus/Terms");
                subFolder1 = subFolderTerms.mkdir();
                File subFolderDocs = new File(postPath + "/StemmedCorpus/Docs");
                subFolder2 = subFolderDocs.mkdir();
                for (char i = 'a'; i <= 'z'; i++) {
                    File Tfolder = new File(postPath + "/StemmedCorpus/Terms/" + i);
                    Tfolder.mkdir();
                    File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
                    merged.createNewFile();
                }
                File Sfolder = new File(postPath + "/StemmedCorpus/Terms/special");
                Sfolder.mkdir();
                File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
                merged.createNewFile();

                new File(subFolderDocs.getPath()+"/docDictionary").mkdir();
                File mergedDoc = new File(subFolderDocs.getPath()+"/docDictionary","docDictionary"+"_merged.txt");
                mergedDoc.createNewFile();
            } else {

                File directory = new File(postPath + "/Corpus");
                corpus = directory.mkdir();
                subFolderTerms = new File(postPath + "/Corpus/Terms");
                subFolder1 = subFolderTerms.mkdir();
                File subFolderDocs = new File(postPath + "/Corpus/Docs");
                subFolder2 = subFolderDocs.mkdir();
                for (char i = 'a'; i <= 'z'; i++) {
                    File Tfolder = new File(postPath + "/Corpus/Terms/" + i);
                    Tfolder.mkdir();
                    File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
                    merged.createNewFile();
                }
                File Sfolder = new File(postPath + "/Corpus/Terms/special");
                Sfolder.mkdir();
                File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
                merged.createNewFile();
                new File(subFolderDocs.getPath()+"/docDictionary").mkdir();
                File mergedDoc = new File(subFolderDocs.getPath()+"/docDictionary","docDictionary"+"_merged.txt");
                mergedDoc.createNewFile();
            }

            if (!corpus || !subFolder1 || !subFolder2) {
                throw new IOException("cannot create directory for indexer corpus");
            }


            if (folder.isDirectory()) {
                File[] listOfSubFolders = folder.listFiles();
                //System.out.println(listOfSubFolders.length);
                List<File> files = new ArrayList<>();
                for (File SubFolder : listOfSubFolders) {
                    if (SubFolder.isDirectory()) {
                        files.add(SubFolder);
                        if (files.size() == 5) {
                            ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stemming, postPath), stemming, corpusPath);
                            executor.execute(new Thread(read));
                            files.clear();
                        }
                        //t.start();
                        //read.run();

                    }

                }
                if (!files.isEmpty()) {
                    ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stemming, postPath), stemming, corpusPath);
                    executor.execute(new Thread(read));
                    files.clear();
                }
                executor.shutdown();
                while (!executor.isTerminated()) {
                }

            }
            executor = Executors.newFixedThreadPool(5);
            for (File file : subFolderTerms.listFiles()) {
                if (file.isDirectory()) {
                    Merge merge = new Merge(file.listFiles());
                    executor.execute(new Thread(merge));
                }
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            Indexer index = new Indexer(comboStemmer.getValue().contains("comboStemmer"), postPath);
            showAlert("numbers of terms = " + index.getNumberOfTerms() + " ,number of documents = " + index.getNumberOrDocuments()
                    + " runTime of the program = " + 0);

            /************************************************//////////////////
        }
    }

    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();
    }

    @FXML
    private void reset() {
        File file = new File(txtPosting.getText());
        String[] lists = file.list();
        if (lists.length > 0) {
            for (String s : lists) {
                File currentFile = new File(file.getPath(), s);
                if (currentFile.isDirectory()) {
                    delete(currentFile);
                }
                currentFile.delete();
            }
        }
        System.gc();
        showAlert("File deleted successfully");
        /*
        if (file.delete()) {
            showAlert("File deleted successfully");
            System.out.println("File deleted successfully");
        } else {
            showAlert("Failed to delete the file");
            System.out.println("Failed to delete the file");
        }*/
    }

    private void delete(File file) {
        String[] lists = file.list();
        if (lists.length > 0) {
            for (String s : lists) {
                File currentFile = new File(file.getPath(), s);
                if (currentFile.isDirectory()) {
                    delete(currentFile);
                }
                currentFile.delete();
            }
        }
    }

    @FXML
    private void displayDictionary() throws IOException {
        String dictionary = "";
        Indexer index = new Indexer(comboStemmer.getValue().contains("comboStemmer"), postPath);
        Set<String> keys = index.getTermDictionary().keySet();
        for (String key : keys) {
            dictionary = dictionary + key + "\n";
        }
        TextArea textArea = new TextArea();
        textArea.setText(dictionary);
        ScrollPane pane = new ScrollPane();
        pane.setContent(textArea);
        System.out.println(dictionary);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dictionary.fxml"));
        Parent tableParent = (Parent) fxmlLoader.load();
        Scene scene = new Scene(pane, 400, 400);
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.prefHeightProperty().bind(scene.heightProperty());
        pane.prefWidthProperty().bind(scene.widthProperty());
        Stage stage = new Stage();
        Stage window = new Stage();
        window.setScene(scene);
        stage.setTitle("Dictionary");
        window.show();
    }

    public void BrowserButtonActionPosting(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            txtPosting.setText(selectedDirectory.getAbsolutePath());
        } else {
            System.out.println("file is not valid");
        }
    }

    @FXML
    private void loadDictionary() throws IOException {
        postPath = txtPosting.getText();
        if (postPath == null) {
            showAlert("please enter posting path");
        } else {
            String dictionary = "";
            Indexer index = new Indexer(comboStemmer.getValue().contains("comboStemmer"), postPath);
            Set<String> keys = index.getTermDictionary().keySet();
            for (String key : keys) {
                dictionary = dictionary + key + "\n";
            }
            File newTextFile = new File(postPath + "/dictionary.txt");
            FileWriter fw = new FileWriter(newTextFile);
            fw.write(dictionary);
            fw.close();
            showAlert("saving completed!");

        }
    }
}
