package Test;

import ViewModel.viewModel;

import java.io.IOException;

public class Test {


    public static void main(String[] args) throws IOException {
        Model_Test2_100DocsTest();

    }


    public static void Model_Test2_100DocsTest() throws IOException {
        String corpusPath = "C:\\Users\\Merav\\Desktop\\SemesterE\\אחזור\\Data\\corpus";
        String resultPath = "C:\\Users\\Merav\\Desktop\\SemesterE\\אחזור\\Result";
        viewModel myModel = new viewModel();
        myModel.start(false, corpusPath,resultPath);
    }


}
