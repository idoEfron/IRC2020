package view;

import Model.Indexer;
import Model.Merge;
import Model.ReadFile;
import ViewModel.viewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
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
    private javafx.scene.control.CheckBox stemmerCheckB;

    /**
     * this function start the program via the view panel
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
            if(corpusInfo.length==1){
                showAlert("file already exist please select the reset button");
                return;

            }
            showAlert("numbers of terms = " + corpusInfo[0] + "\nnumber of documents = " + corpusInfo[1]
                    + "\nrunTime of the program = " + ((endTime - startTime) / 1000) + " seconds");
        }
    }

    /**
     * this function responsible of showing the alert
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

    /**
     * set the browser to the users wanted path in view panel
     * @param txt
     */
    private void browser(TextArea txt) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            txt.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * this is a controller function to show the dictionary in view panel
     * @throws IOException
     */
    @FXML
    private void displayDictionary() throws IOException {
        String dictionary = "";
        File file = null;
        if (postPath != null) {
            file = new File(txtPosting.getText());
        }
        if (txtPosting.getText().length() > 0 && file != null) {
            dictionary = viewModel.displayDictionary(stemmerCheckB.isSelected(), txtPosting.getText());
            if (dictionary.length() > 0) {
                TextArea textArea = new TextArea();
                textArea.setText(dictionary);
                ScrollPane pane = new ScrollPane();
                pane.setContent(textArea);
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
            } else {
                showAlert("There's no dictionary to display");
            }
        } else {
            showAlert("please enter correct posting path");
        }
    }

    /**
     * this function load the dictionary from hard disk via the view panel
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
}
