package Model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Merge implements Runnable {

    File[] directory;

    /**
     * this is the constructor of this class
     * @param files
     */
    public Merge(File[] files){
        directory = files;
    }

    /**
     * this function merge all the files in the folder to one folder
     * @param files the list of files
     * @throws IOException
     */
    private void merge(File[] files) throws IOException {
        List<String> mergedText = new ArrayList<>();
        String parent = files[0].getParent();
        File merged = new File(parent+parent.substring(parent.lastIndexOf('\\'))+"_merged.txt");
        for (File file : files) {
            if (file.isDirectory()) {

                //System.out.println("Directory: " + file.getName());

                merge(file.listFiles()); // Calls same method again.
            } else {
                if(!file.getName().contains("merged.txt")){
                    List<String> text = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                    mergedText.addAll(text);
                    file.delete();
                }
            }
        }
        if(mergedText!=null && mergedText.size()>0){
            mergedText.addAll(Files.readAllLines(merged.toPath()));
            Collections.sort(mergedText,String.CASE_INSENSITIVE_ORDER);
            for (int i=mergedText.size()-1;i>=0;i--){
                String term = mergedText.get(i).substring(0,mergedText.get(i).indexOf(':'));
                if(Character.isLowerCase(term.charAt(0))){
                    if(i>0){
                        if(Character.isUpperCase(mergedText.get(i-1).charAt(0))){
                            if(term.toLowerCase().equals(mergedText.get(i-1).substring(0,mergedText.get(i-1).indexOf(':')).toLowerCase())){
                                String suffix = mergedText.get(i-1).substring(mergedText.get(i-1).indexOf(": ")+2);
                                mergedText.set(i,mergedText.get(i)+suffix);
                                mergedText.remove(i-1);
                            }
                        }
                        else{
                            mergeHelper(mergedText, i, term);
                        }
                    }
                }
                else{
                    if(i>0){
                        mergeHelper(mergedText, i, term);
                    }
                }

            }
            writeRaw(mergedText,merged.getPath());
            mergedText.clear();
        }
    }

    private void mergeHelper(List<String> mergedText, int i, String term) {
        if(term.toLowerCase().equals(mergedText.get(i-1).substring(0,mergedText.get(i-1).indexOf(':')).toLowerCase())){
            String suffix = mergedText.remove(i).substring(mergedText.get(i-1).indexOf(": ")+2);
            mergedText.set(i-1,mergedText.get(i-1)+suffix);
        }
    }

    /**
     * this function write in in a text file all the records
     * @param records the records that needed to be saved
     * @param filePath the file path
     * @throws IOException
     */
    private static void writeRaw(List<String> records,String filePath) throws IOException {
        File file = new File( filePath );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        write(records, writer);
    }

    /**
     * this function assisting to the writeRaw and saving the file object
     * @param records the recodes list that needed to be saved
     * @param writer the object that save the object
     * @throws IOException
     */
    private static void write(List<String> records, Writer writer) throws IOException {
        for (String record: records) {
            writer.write(record+'\n');
        }
        writer.flush();
        writer.close();
    }

    /**
     * this fumction tun the merge function
     */
    @Override
    public void run() {
        try {
            merge(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
