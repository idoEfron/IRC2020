package Model;

import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import javafx.util.Pair;

import javax.annotation.processing.FilerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Indexer {

    private static Map<String, Map<String,Integer>> termDictionary = new TreeMap<>();
    private static HashMap<String, String> docDictionary = new HashMap<>();
    private File subFolderTerms;
    private File subFolderDocs;
    private static Semaphore mutex = new Semaphore(1);

    public int getNumberOfTerms(){
        return termDictionary.size();
    }
    public  int getNumberOrDocuments(){
        return docDictionary.size();
    }

    public Indexer(boolean stem, String postingPath){

        if(!stem){
            subFolderTerms = new File(postingPath+"/Corpus/Terms");
            subFolderDocs= new File(postingPath+"/Corpus/Docs");
        }
        else{
            subFolderTerms = new File(postingPath+"/StemmedCorpus/Terms");
            subFolderDocs= new File(postingPath+"/StemmedCorpus/Docs");
        }

    }

    public boolean addBlock(Parser p) throws IOException, InterruptedException {
        boolean createdFile;
        File file = null;
        File currentFile=null;
        FileWriter filewriter = null;
        BufferedWriter bw =null;
        PrintWriter writer=null;

        Map <Token,Map<String,ArrayList<String>>> termMap = p.getTermMap();
        Set<Token> tknSet = termMap.keySet();


        long totalTime=0;

        Map <String ,List<String>> lines = new HashMap<>();

        for (Token tkn : tknSet) {
            if(Character.isLetter(tkn.getStr().charAt(0))){
                if(lines.containsKey(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt")){
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,ArrayList<String>>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry<String,ArrayList<String>> me : map){
                        lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("<"+me.getKey()+"|" + me.getValue().get(0)+"|"+me.getValue().get(1)+"|"+me.getValue().get(2)+">");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("\n");

                }
                else{
                    lines.put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt",new ArrayList<String>());
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,ArrayList<String>>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry<String,ArrayList<String>> me : map){
                        lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("<"+me.getKey()+"|" + me.getValue().get(0)+"|"+me.getValue().get(1)+"|"+me.getValue().get(2) +">");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("\n");
                }

            }
            else{
                if(lines.containsKey(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt")){
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,ArrayList<String>>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry<String,ArrayList<String>> me : map){
                        lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("<"+me.getKey()+"|" + me.getValue().get(0)+"|"+me.getValue().get(1)+"|"+me.getValue().get(2) +">");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("\n");
                }
                else{
                    lines.put(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt",new ArrayList<String>());
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,ArrayList<String>>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry<String,ArrayList<String>> me : map){
                        lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("<"+me.getKey()+"|" + me.getValue().get(0)+"|"+me.getValue().get(1)+"|"+me.getValue().get(2) +">");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("\n");
                }
            }
            mutex.acquire();
            if(!termDictionary.containsKey(tkn.getStr().toUpperCase()) && !termDictionary.containsKey(tkn.getStr().toLowerCase())){
                if(Character.isLetter(tkn.getStr().charAt(0))){
                    if(Character.isUpperCase(tkn.getStr().charAt(0))){
                        termDictionary.put(tkn.getStr().toUpperCase(),new HashMap<>());
                        termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                    }
                    else{
                        termDictionary.put(tkn.getStr().toLowerCase(),new HashMap<>());
                        termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                    }
                }
                else{
                    termDictionary.put(tkn.getStr(),new HashMap<>());
                    termDictionary.get(tkn.getStr()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));

                }
            }
            else{
                if(Character.isLetter(tkn.getStr().charAt(0))){
                    if(termDictionary.containsKey(tkn.getStr().toUpperCase())){
                        if(Character.isUpperCase(tkn.getStr().charAt(0))) {
                            termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                        }
                        else{
                            termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                            termDictionary.put(tkn.getStr().toLowerCase(),termDictionary.remove(tkn.getStr().toUpperCase()));
                        }
                    }
                    else{
                        termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                    }
                }
                else{
                    termDictionary.get(tkn.getStr()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                }
            }
            mutex.release();
        }
        for (String str:lines.keySet()){
            writeRaw(lines.get(str),str);
        }

        lines.clear();
        termMap.clear();
        tknSet.clear();

        for (String docID : p.getWordCounter().keySet()) {
            file = new File(subFolderDocs.getPath() + "/" + docID + ".txt");
            if (!file.exists()) {
                createdFile = file.createNewFile();
                if (!createdFile) {
                    throw new FilerException("cannot create file for indexer corpus" + docID);
                }
                docDictionary.put(docID, file.getPath());
            }
            filewriter = new FileWriter(file, true);
            bw = new BufferedWriter(filewriter);
            writer = new PrintWriter(bw);

            writer.print(p.getMaxTf().get(docID) + "," + p.getWordCounter().get(docID) + ">>");
            writer.close();
        }

        return true;
    }

    private Integer sumTf(String term,Collection<Map.Entry<String,ArrayList<String>>> values) {
        Integer sum;
        if((termDictionary.containsKey(term.toLowerCase()) && termDictionary.get(term.toLowerCase()).size()>0)){
            sum = termDictionary.get(term.toLowerCase()).get(termDictionary.get(term.toLowerCase()).keySet().toArray()[0]);
        }
        if((termDictionary.containsKey(term.toUpperCase()) && termDictionary.get(term.toUpperCase()).size()>0)) {
            if (Character.isLowerCase(term.charAt(0))) {
                sum = termDictionary.get(term.toUpperCase()).remove(termDictionary.get(term.toUpperCase()).keySet().toArray()[0]);
            } else {
                sum = termDictionary.get(term.toUpperCase()).get(termDictionary.get(term.toUpperCase()).keySet().toArray()[0]);
            }
        }

        else{
            sum =0;
        }
        for(Map.Entry<String,ArrayList<String>> num:values){
            sum = sum+ Integer.parseInt(num.getValue().get(0));
        }
        return sum;
    }

    //https://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java

    public int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];

            int readChars = is.read(c);
            if (readChars == -1) {
                // bail out if nothing to read
                return 0;
            }

            // make it easy for the optimizer to tune this loop
            int count = 0;
            while (readChars == 1024) {
                for (int i = 0; i < 1024; ) {
                    if (c[i++] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            // count remaining characters
            while (readChars != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            int k = 0;
            return count;
        } finally {
            is.close();
        }
    }



    //https://stackoverflow.com/questions/5600422/method-to-find-string-inside-of-the-text-file-then-getting-the-following-lines/45168182
    public int getLineNum(String term, String path) {
        File file = new File(path);

        try {
            Scanner scanner = new Scanner(file);

            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                if (line.contains(term)) {
                    return lineNum;

                }
            }
        } catch (FileNotFoundException e) {
            return -1;
        }
        return -1;
    }

    public void addToLine(File file, int line, File folder) throws IOException {


        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        Collections.sort(lines);
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

    }

    //https://stackoverflow.com/questions/1062113/fastest-way-to-write-huge-data-in-text-file-java
    private static void writeRaw(List<String> records,String filePath) throws IOException {
        File file = new File( filePath );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        write(records, writer);
    }

    private static void write(List<String> records, Writer writer) throws IOException {
        for (String record: records) {
            writer.write(record);
        }
        writer.flush();
        writer.close();
    }


    public static Map<String, Map<String,Integer>> getTermDictionary() {
        return termDictionary;
    }
    public static void clearMap(){
        termDictionary.clear();
    }
    public static void setTermDictionary(Map<String, Map<String, Integer>> termDictionary) {
        Indexer.termDictionary = termDictionary;
    }
}
