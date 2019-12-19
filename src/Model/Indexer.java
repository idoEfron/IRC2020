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

    private static Map<String, Map<String,Integer>> termDictionary = new HashMap<>();
    private static HashMap<String, String> docDictionary = new HashMap<>();
    private File directory;
    private File subFolderTerms;
    private File subFolderDocs;
    private static Semaphore mutex = new Semaphore(1);
    private static int fileNum =0;
    private static int folderNum=0;

    /**
     * 
     * this function is a getter
     * @return the size of the termDictinary
     */
    public int getNumberOfTerms(){
        return termDictionary.size();
    }

    /**
     * this function is a getter that returns the docDictionary size
     * @return docDictionary size
     */
    public  int getNumberOrDocuments(){
        return docDictionary.size();
    }

    /**
     * this function is a constructor of indexer
     * @param stem if stamming option is selected
     * @param postingPath  the path of posting file location
     * @throws IOException exception
     */
    public Indexer(boolean stem, String postingPath) throws IOException {

        if(!stem){
            subFolderTerms = new File(postingPath+"/Corpus/Terms");
            subFolderDocs= new File(postingPath+"/Corpus/Docs");
        }
        else{
            subFolderTerms = new File(postingPath+"/StemmedCorpus/Terms");
            subFolderDocs= new File(postingPath+"/StemmedCorpus/Docs");
        }

    }

    /**
     * this function puts the data of the parser to files of posting and put data on the memory
     * @param p the parser
     * @return true or false if the block was index successfuly
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean addBlock(Parser p) throws IOException, InterruptedException {
        boolean createdFile;
        File file = null;
        File currentFile=null;
        FileWriter filewriter = null;
        BufferedWriter bw =null;
        PrintWriter writer=null;

        Map <Token,Map<String,Integer>> termMap = p.getTermMap();
        Set<Token> tknSet = termMap.keySet();


        long totalTime=0;

        Map <String ,List<String>> lines = new HashMap<>();

        for (Token tkn : tknSet) {
            if(Character.isLetter(tkn.getStr().charAt(0))){
                if(lines.containsKey(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt")){
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,Integer>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry me : map){
                        lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(me.getKey()+"-" + me.getValue() +">> ");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("\n");

                }
                else{
                    lines.put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt",new ArrayList<String>());
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,Integer>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry me : map){
                        lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add(me.getKey()+"-" + me.getValue() +">> ");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt").add("\n");
                }

            }
            else{
                if(lines.containsKey(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt")){
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,Integer>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry me : map){
                        lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(me.getKey()+"-" + me.getValue() +">> ");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("\n");
                }
                else{
                    lines.put(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt",new ArrayList<String>());
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
                    Set <Map.Entry<String,Integer>> map = termMap.get(tkn).entrySet();
                    for (Map.Entry me : map){
                        lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add(me.getKey()+"-" + me.getValue() +">> ");
                    }
                    lines.get(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt").add("\n");
                }
            }
            mutex.acquire();
            if(!termDictionary.containsKey(tkn.getStr().toUpperCase()) && !termDictionary.containsKey(tkn.getStr().toLowerCase())){
                if(Character.isUpperCase(tkn.getStr().charAt(0))){
                    termDictionary.put(tkn.getStr().toUpperCase(),new HashMap<>());
                    termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).values()));
                }
                else{
                    termDictionary.put(tkn.getStr().toLowerCase(),new HashMap<>());
                    termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).values()));
                }
            }
            else{
                if(termDictionary.containsKey(tkn.getStr().toUpperCase())){
                    if(Character.isUpperCase(tkn.getStr().charAt(0))) {
                        termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).values()));
                    }
                    else{
                        termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).values()));
                        termDictionary.put(tkn.getStr().toLowerCase(),termDictionary.remove(tkn.getStr().toUpperCase()));
                    }
                }
                else{
                    termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",sumTf(tkn.getStr(),termMap.get(tkn).values()));
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

    /**
     * sum all of the tf of a term in all of the corpus
     * @param term the current term that we want to know the tf
     * @param values all of the tf in different in documents
     * @return the tf of the term
     */
    private Integer sumTf(String term,Collection<Integer> values) {
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
        for(Integer num:values){
            sum = sum+ num;
        }
        return sum;
    }

    //https://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java

    /**
     *
     * @param filename
     * @return
     * @throws IOException
     */
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


    /**
     *
     * @param term
     * @param path
     * @return
     */
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

    /**
     *this function write the records to a file/
     * @param records
     * @param filePath
     * @throws IOException
     */
    //https://stackoverflow.com/questions/1062113/fastest-way-to-write-huge-data-in-text-file-java
    private static void writeRaw(List<String> records,String filePath) throws IOException {
        File file = new File( filePath );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        write(records, writer);
    }

    /**
     * this function is a continues function to WrithRaw.
     * @param records the records that we need to save.
     * @param writer the writer option that saving the record.
     * @throws IOException
     */
    private static void write(List<String> records, Writer writer) throws IOException {
        for (String record: records) {
            writer.write(record);
        }
        writer.flush();
        writer.close();
    }

    /**
     *a getter that return the termDictionary
     * @return the termDictionary
     */
    public static Map<String, Map<String,Integer>> getTermDictionary() {
        return termDictionary;
    }

    /**
     * this function is cleaning the data structure
     */
    public static void clearMap(){
        termDictionary.clear();
        docDictionary.clear();

    }

    /**
     * this function is a setter that save the term Dictionary
     * @param termDictionary
     */
    public static void setTermDictionary(Map<String, Map<String, Integer>> termDictionary) {
        Indexer.termDictionary = termDictionary;
    }
}
