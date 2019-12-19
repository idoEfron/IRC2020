package Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

public class ReadFile implements  Runnable{
    //protected ArrayList<String> allFile;
    private List<File> subFolder;
    private String[] splits;
    private Indexer index;
    private boolean stem;
    String stopWordsPath;

    /**
     * this is the constructor of the read file
     * @param subFolder the folder that the read file reads
     * @param i the indexer object
     * @param stemming the boolean of the stemming option
     * @param stopWordsPath the string of the stop words
     * @throws IOException
     */
    public ReadFile(List<File> subFolder,Indexer i,boolean stemming,String stopWordsPath) throws IOException {
        //allFile = new ArrayList<>();
        this.subFolder = subFolder;
        index=i;
        stem = stemming;
        this.stopWordsPath = stopWordsPath;
    }

    /**
     * this function reads the file in a giving corpus path
     */
    @Override
    public void run() {
        Scanner file3 = null;
        int counter = 0;//delete
        File[] files1 = null;
        String text = "";
        for(File file: subFolder){
            files1 = file.listFiles();
            for (File file2 : files1) {
                String TxtPaths = file2.getPath();
                try {
                    file3 = new Scanner(file2);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                finally {
                    file3.close();
                }
                try {
                    text =text+ new String(Files.readAllBytes(Paths.get(TxtPaths)), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                splits = text.split("</DOC>");
                //allFile.addAll(Arrays.asList(splits));//
                counter = counter + splits.length - 1;//delete
             }
        }
        try {
            Parser p = new Parser(stem,this,stopWordsPath,index);
                p.parseDocs(splits);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //allFile.clear();

    }

    /**
     * this function is a getter that gets the subFolder list
     * @return
     */
    public List<File> getSubFolder() {
        return subFolder;
    }

    /**
     * this function is a setter thats set the subFolder list
     * @param subFolder
     */
    public void setSubFolder(List<File> subFolder) {
        this.subFolder = subFolder;
    }

}
