package view;

import Model.Indexer;
import Model.Merge;
import Model.ReadFile;
import ViewModel.viewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private viewModel viewModel = new viewModel();
    private String postPath = "";
    @FXML
    private javafx.scene.control.Button broeseB;
    @FXML
    private javafx.scene.control.Button resetB;
    @FXML
    private TextArea txtBrowse;
    @FXML
    private TextArea txtPosting;
    @FXML
    private TextArea txtQuery;
    @FXML
    private javafx.scene.control.CheckBox stemmerCheckB;
    @FXML
    private TextArea txtQueryPath;
    @FXML
    private ComboBox<String> comboBox;
    private ObservableList<String> list = FXCollections.observableArrayList();
    @FXML
    private javafx.scene.control.CheckBox semanticCheckB;
    @FXML
    javafx.scene.control.CheckBox descripChoice;

    /**
     * this function start the program via the view panel
     *
     * @throws IOException
     */
    @FXML
    private void startIR() throws IOException {
        long startTime = System.currentTimeMillis();
        boolean stem = stemmerCheckB.isSelected();
        String postPath = txtPosting.getText();
        String corpusPath = txtBrowse.getText();

        if (postPath.equals("")) {
            showAlert("please enter posting path");
            return;

        }
        if (corpusPath.equals("")) {
            showAlert("please enter corpus path");
            return;

        }
        if (corpusPath != null && postPath != null) {
            File postDir = new File(postPath);
            File corpusdir = new File(corpusPath);
            if (postDir.isDirectory() == false) {
                showAlert("please enter correct posting path");
                return;
            }
            if (corpusdir.isDirectory() == false) {
                showAlert("please enter correct corpus path");
                return;
            }
            int[] corpusInfo = viewModel.start(stem, postPath, corpusPath);
            long endTime = System.currentTimeMillis();
            if (corpusInfo.length == 1) {
                showAlert("file already exist please select the reset button");
                return;

            }
            showAlert("numbers of terms = " + corpusInfo[0] + "\nnumber of documents = " + corpusInfo[1]
                    + "\nrunTime of the program = " + ((endTime - startTime) / 1000) + " seconds");

        }
    }

    /**
     * this function responsible of showing the alert
     *
     * @param alertMessage the massage to be shown
     */
    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();
    }

    /**
     * reset all memory saved on RAM and on hard disk
     */
    @FXML
    private void reset() {
        if (txtPosting.getText().equals("")) {
            showAlert("please enter correct posting path");
            return;
        } else {
            Indexer.clearMap();
            File file = new File(txtPosting.getText());
            if (file.list().length > 0) {
                viewModel.delete(file);
                System.gc();
                txtPosting.clear();
                txtBrowse.clear();
                ReadFile.setDocs(0);
                comboBox.getItems().removeAll();
                showAlert("File deleted successfully");
            } else {
                showAlert("Their is no files to delete");
                return;
            }
        }

    }

    @FXML
    public void BrowserButtonActionPosting(ActionEvent event) {
        browser(txtPosting);
    }

    @FXML
    public void BrowserButtonAction(ActionEvent event) {
        browser(txtBrowse);
    }

    @FXML
    public void BrowserButtonActionQuery(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            txtQueryPath.setText(selectedFile.getPath());
            System.out.println(("File selected: " + selectedFile.getName()));
        } else {
            System.out.println("File selection cancelled.");
        }
    }


    /**
     * set the browser to the users wanted path in view panel
     *
     * @param txt
     */
    private void browser(TextArea txt) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            txt.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    public void startQuery() throws IOException, ParseException, InterruptedException {
        if(Indexer.getTermDictionary().size()==0||Indexer.getDocDictionary().size()==0){
            showAlert("please run OR load corpus");
            return;
        }
        if (txtQueryPath.getText() == null || txtQueryPath.getText().equals("")) {
            showAlert("please enter queries path");
            return;
        }
        if (txtPosting.getText() == null || txtPosting.getText().equals("")) {
            showAlert("no posting ");
            return;
        }
        if (txtBrowse.getText() == null || txtBrowse.getText().equals("")) {
            showAlert("no corpus ");
            return;
        }
        String path = txtQueryPath.getText();
        //Map<String,Map<String,Double>> finalDocs = viewModel.startQuery(path,txtBrowse.getText(),stemmerCheckB.isSelected(),semanticCheckB.isSelected());
        List<String> outDisplay = viewModel.startQuery(path, txtBrowse.getText(), stemmerCheckB.isSelected(), semanticCheckB.isSelected(), descripChoice.isSelected());
        if (outDisplay != null && outDisplay.size() > 0) {
            ObservableList<String> observableList = FXCollections.observableList(outDisplay);
            ListView listView = new ListView<>();
            listView.setItems(observableList);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dictionary.fxml"));
            Parent tableParent = (Parent) fxmlLoader.load();
            Scene scene = new Scene(listView, 450, 400);
            Stage stage = new Stage();
            Stage window = new Stage();
            window.setScene(scene);
            stage.setTitle("queries");
            window.show();
            if (viewModel.getDocumentInQuery().size() > 0 && viewModel.getDocumentInQuery() != null) {
                comboBox.getItems().removeAll();
                comboBox.getItems().addAll(viewModel.getDocumentInQuery().stream().sorted().collect(Collectors.toList()));
            }
        } else {
            showAlert("There's nothing to display");
        }
    }

    @FXML
    public void displayTopFive() throws IOException {
        if(comboBox.getValue()==null){
            showAlert("Please choose document/run query");
            return;
        }
        if (comboBox.getValue() != null || comboBox.getValue().equals("")) {
            String docComo = comboBox.getValue();
            if (txtPosting.getText() == null || txtPosting.getText().equals("")) {
                showAlert("please enter posting path");
            } else {
                String path = txtPosting.getText() + "/docDictionary.txt";
                readDoc(docComo, path);
            }
        }
    }

    private void readDoc(String docComo, String stemmerPath) throws IOException {
        File topFiveStem = new File(stemmerPath);
        if (topFiveStem.exists()) {
            List<String> text = Files.readAllLines(topFiveStem.toPath(), StandardCharsets.UTF_8);
            for (String line : text) {
                String[] lineArr = line.split(">");
                if (lineArr.length == 3 && lineArr[0].equals(docComo)) {
                    String ent = lineArr[2];
                    ent = ent.replace("[", "");
                    ent = ent.replace("]", "");
                    String[] entArr = ent.split(", ");
                    String out = "top entities for the document " + docComo + " are:";
                    for (int i = 0; i < entArr.length; i++) {
                        out = out + " " + entArr[i] + "\n";
                    }
                    showAlert(out);
                }
            }
        }
    }

    @FXML
    public void startSingleQuery() throws IOException, ParseException, InterruptedException {
        List<String> outDisplay = new LinkedList<>();
        String query = txtQuery.getText();
        if(Indexer.getTermDictionary().size()==0||Indexer.getDocDictionary().size()==0){
            showAlert("please run OR load corpus");
            return;
        }
        if (txtPosting.getText() == null) {
            showAlert("no posting ");
            return;

        }
        if (txtBrowse.getText() == null) {
            showAlert("no corpus ");
            return;
        }
        if (txtQuery.getText() == null) {
            showAlert("please enter query");
            return;
        }
        if (!txtQuery.getText().contains("<num>") || !txtQuery.getText().contains("<title> ")) {
            showAlert("please enter correct query");
            return;
        }
        outDisplay = viewModel.startSingleQuery(query, txtBrowse.getText(), stemmerCheckB.isSelected(), semanticCheckB.isSelected(), descripChoice.isSelected());
        if (outDisplay != null && outDisplay.size() > 0) {
            ObservableList<String> observableList = FXCollections.observableList(outDisplay);
            ListView listView = new ListView<>();
            listView.setItems(observableList);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dictionary.fxml"));
            Parent tableParent = (Parent) fxmlLoader.load();
            Scene scene = new Scene(listView, 400, 400);
            Stage stage = new Stage();
            Stage window = new Stage();
            window.setScene(scene);
            stage.setTitle("queries");
            window.show();
            if (viewModel.getDocumentInQuery().size() > 0 && viewModel.getDocumentInQuery() != null) {
                comboBox.getItems().removeAll();
                comboBox.getItems().addAll(viewModel.getDocumentInQuery());
            }
        } else {
            showAlert("There's nothing to display");
        }
        //comboBox.getItems().addAll(comoList);
    }
    @FXML
    public void saveResult() throws IOException {
        String path = "";
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            path = selectedDirectory.getAbsolutePath();
            if (viewModel.getDocsRanks()!=null&&viewModel.getDocsRanks().size() == 0) {
                showAlert("please run query");
                return;
            }
            viewModel.writeToResultFile(path);
            showAlert("Result have neen saved!!:)");
        }
        showAlert("please select correct path");
    }
    /**
     * this is a controller function to show the dictionary in view panel
     *
     * @throws IOException
     */
    @FXML
    private void displayDictionary() throws IOException {
        List<String> dictionary = new LinkedList<>();
        File file = null;
        if (postPath != null) {
            file = new File(txtPosting.getText());
        }
        if (txtPosting.getText().length() > 0 && file != null) {
            dictionary = viewModel.displayDictionary(stemmerCheckB.isSelected(), txtPosting.getText());
            if (dictionary != null && dictionary.size() > 0) {
                ObservableList<String> observableList = FXCollections.observableList(dictionary);
                ListView listView = new ListView<>();
                listView.setItems(observableList);
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dictionary.fxml"));
                Parent tableParent = (Parent) fxmlLoader.load();
                Scene scene = new Scene(listView, 400, 400);
                Stage stage = new Stage();
                Stage window = new Stage();
                window.setScene(scene);
                stage.setTitle("Dictionary");
                window.show();
            } else {
                showAlert("There's no dictionary to display");
            }
        } else {
            showAlert("please enter correct posting path");
        }
    }

    /**
     * this function load the dictionary from hard disk via the view panel
     *
     * @throws IOException
     */


    @FXML
    private void loadDictionary() throws IOException {
        postPath = txtPosting.getText();
        if (postPath == null || postPath.equals("")) {
            showAlert("please enter posting path");
            return;
        } else {
            viewModel.loadDictionary(stemmerCheckB.isSelected(), postPath);
            String dictionary = "";
            showAlert("saving completed!");

        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBox.setItems(list);
    }
}
