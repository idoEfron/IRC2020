package Model;


import org.omg.PortableInterceptor.INACTIVE;

import javax.annotation.processing.FilerException;
import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Indexer {

    private static Map<String, Map<String,ArrayList<Integer>>> termDictionary = new TreeMap<>();
    private static HashMap<String, String> docDictionary = new HashMap<>();
    private String postingPath;//todo ido add!!!!!!!!!!!!!!!!!!!!!!!!!!
    private File subFolderTerms;
    private File subFolderDocs;
    //private static Semaphore mutex = new Semaphore(1);
    private static double totalDocLength;

    public static HashMap<String, String> getDocDictionary() {
        return docDictionary;
    }
    public static double getTotalDocLength() {
        return totalDocLength;
    }

    /**
     *
     * this function returns the number of terms saved
     * @return the size of the termDictinary
     */
    public int getNumberOfTerms(){
        return termDictionary.size();
    }

    /**
     * this function returns the number of documents saved
     * @return size of docDictionary
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
    public Indexer(boolean stem, String postingPath) {
        this.postingPath = postingPath;
        if(!stem){
            subFolderTerms = new File(postingPath+"/Corpus/Terms");
            subFolderDocs= new File(postingPath+"/Corpus/Docs");
        }
        else{
            subFolderTerms = new File(postingPath+"/StemmedCorpus/Terms");
            subFolderDocs= new File(postingPath+"/StemmedCorpus/Docs");
        }

    }

    public void lineMap(Token tkn,Map <String ,List<String>> lines,Map<Token, Map<String, ArrayList<String>>> termMap){

        if(Character.isLetter(tkn.getStr().charAt(0))){
            if(lines.containsKey(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt")){
                insertToLines(tkn,termMap,lines,Character.toString(tkn.getStr().toLowerCase().charAt(0)));
            }
            else{
                lines.put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getFile()+".txt",new ArrayList<String>());
                insertToLines(tkn,termMap,lines,Character.toString(tkn.getStr().toLowerCase().charAt(0)));

            }

        }
        else{
            if(lines.containsKey(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt")){
                insertToLines(tkn,termMap,lines,"special");
            }
            else{
                lines.put(subFolderTerms.getPath()+"/"+"special/"+tkn.getFile()+".txt",new ArrayList<String>());
                insertToLines(tkn,termMap,lines,"special");
            }
        }
    }

    /**
     * todo ido add!!!!!!!!!!!!!!!!!!!!!!
     * @return
     */
    public String getPostingPath() {
        return postingPath;
    }

    /**
     * this function puts the data of the parser to files of posting and put data on the memory
     * @param p the parser
     * @return true or false if the block was index successfuly
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean addBlock(Parser p) throws IOException, InterruptedException {

        //mutex.acquire();
        Map<String, Integer> docLength = p.getDocLength();
        for(String docID : docLength.keySet()){
            totalDocLength = totalDocLength+docLength.get(docID);
        }
        boolean createdFile;
        File file = null;

        FileWriter filewriter = null;
        BufferedWriter bw =null;
        PrintWriter writer=null;

        Map <Token,Map<String,ArrayList<String>>> termMap = p.getTermMap();
        Set<Token> tknSet = termMap.keySet();

        Map <String ,List<String>> lines = new HashMap<>();

        for (Token tkn : tknSet) {

            if(!termDictionary.containsKey(tkn.getStr().toUpperCase()) && !termDictionary.containsKey(tkn.getStr().toLowerCase())){
                if(Character.isLetter(tkn.getStr().charAt(0))){
                    if(Character.isUpperCase(tkn.getStr().charAt(0))){
                        termDictionary.put(tkn.getStr().toUpperCase(),new HashMap<>());
                        termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",new ArrayList<>());
                        termDictionary.get(tkn.getStr().toUpperCase()).get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                        lineMap(tkn,lines,termMap);
                    }
                    else{
                        termDictionary.put(tkn.getStr().toLowerCase(),new HashMap<>());
                        termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",new ArrayList<>());
                        termDictionary.get(tkn.getStr().toLowerCase()).get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                        lineMap(tkn,lines,termMap);
                    }
                }
                else{
                    if(tkn.getStr().contains("-")){
                        String[] strA = tkn.getStr().split("-");
                        if(strA.length==2){
                            if(Character.isLetter(strA[1].charAt(0))){
                                if(Character.isUpperCase(strA[1].charAt(0))){
                                    termDictionary.put(tkn.getStr().toUpperCase(),new HashMap<>());
                                    termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",new ArrayList<>());
                                    termDictionary.get(tkn.getStr().toUpperCase()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                                    lineMap(tkn,lines,termMap);
                                }
                                else{
                                    termDictionary.put(tkn.getStr().toLowerCase(),new HashMap<>());
                                    termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",new ArrayList<>());
                                    termDictionary.get(tkn.getStr().toLowerCase()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                                    lineMap(tkn,lines,termMap);
                                }
                            }
                            else{
                                termDictionary.put(tkn.getStr(),new HashMap<>());
                                termDictionary.get(tkn.getStr()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",new ArrayList<>());
                                termDictionary.get(tkn.getStr()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                                lineMap(tkn,lines,termMap);

                            }
                        }
                    }
                    else{
                        termDictionary.put(tkn.getStr(),new HashMap<>());
                        termDictionary.get(tkn.getStr()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",new ArrayList<>());
                        termDictionary.get(tkn.getStr()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").add(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                        lineMap(tkn,lines,termMap);
                    }
                }
            }
            else{
                if(Character.isLetter(tkn.getStr().charAt(0))){
                    if(termDictionary.containsKey(tkn.getStr().toUpperCase())){
                        if(Character.isUpperCase(tkn.getStr().charAt(0))) {
                            //termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",new ArrayList<>());
                            termDictionary.get(tkn.getStr().toUpperCase()).get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt").set(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                            lineMap(tkn,lines,termMap);
                        }
                        else{
                            //termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt", new ArrayList<>());
                            termDictionary.get(tkn.getStr().toUpperCase()).get(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt").set(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                            termDictionary.put(tkn.getStr().toLowerCase(),termDictionary.remove(tkn.getStr().toUpperCase()));
                            lineMap(tkn,lines,termMap);
                        }
                    }
                }
                else{
                    try{
                        if(tkn.getStr().contains("-")){
                            if(termDictionary.containsKey(tkn.getStr().toUpperCase())){
                                //termDictionary.get(tkn.getStr().toUpperCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",new ArrayList<>());
                                termDictionary.get(tkn.getStr().toUpperCase()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").set(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                                lineMap(tkn,lines,termMap);
                            }
                            else{
                                //termDictionary.get(tkn.getStr().toLowerCase()).put(subFolderTerms.getPath()+"/"+tkn.getStr().toLowerCase().charAt(0)+"/"+tkn.getStr().toLowerCase().charAt(0)+"_merged.txt",new ArrayList<>());
                                termDictionary.get(tkn.getStr().toLowerCase()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").set(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                                lineMap(tkn,lines,termMap);
                            }
                        }
                        else{
                            //termDictionary.get(tkn.getStr()).put(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt",new ArrayList<>());
                            termDictionary.get(tkn.getStr()).get(subFolderTerms.getPath()+"/"+"special/"+"special_merged.txt").set(0,sumTf(tkn.getStr(),termMap.get(tkn).entrySet()));
                            lineMap(tkn,lines,termMap);
                        }
                    }
                    catch(NullPointerException e){
                        System.out.println("the term is " +tkn.getStr());
                    }
                }
            }
           // mutex.release();
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

            // posting line format: maxTf,numOfUniqueWords,docLength
            writer.print(p.getMaxTf().get(docID) + "," + p.getWordCounter().get(docID) + ","+p.getDocLength().get(docID));
            writer.close();
        }

        return true;
    }

    /**
     * this function insert strings to lines ArrayList, which will finally become the lines in the written inverted file
     * @param tkn the term we want to index and its details
     * @param termMap the parsed terms and their details
     * @param lines arraylist of lines that are written to the inverted file
     * @param folder the folder to which we save the appropriate file
     */

    private void insertToLines(Token tkn, Map <Token,Map<String,ArrayList<String>>> termMap,Map <String ,List<String>> lines,String folder){
        lines.get(subFolderTerms.getPath()+"/"+folder+"/"+tkn.getFile()+".txt").add(tkn.getStr() +" : ");
        Set <Map.Entry<String,ArrayList<String>>> map = termMap.get(tkn).entrySet();
        for (Map.Entry<String,ArrayList<String>> me : map){
            lines.get(subFolderTerms.getPath()+"/"+folder+"/"+tkn.getFile()+".txt").add("<"+me.getKey()+"|" + me.getValue().get(0)+"|"+me.getValue().get(1)+"|"+me.getValue().get(2)+">");
        }
        lines.get(subFolderTerms.getPath()+"/"+folder+"/"+tkn.getFile()+".txt").add("\n");
    }

    /**
     * this function sums the total tf of a term
     * @param term the term we want to count its tf
     * @param values the collection that contain the tf information
     * @return int representing the total tf of the term
     */

    private Integer sumTf(String term,Collection<Map.Entry<String,ArrayList<String>>> values) {
        Integer sum;
        if((termDictionary.containsKey(term.toLowerCase()) && termDictionary.get(term.toLowerCase()).size()>0 && termDictionary.get(term.toLowerCase()).get(termDictionary.get(term.toLowerCase()).keySet().toArray()[0]).size()>0)){
            sum = termDictionary.get(term.toLowerCase()).get(termDictionary.get(term.toLowerCase()).keySet().toArray()[0]).get(0);
        }
        if((termDictionary.containsKey(term.toUpperCase()) && termDictionary.get(term.toUpperCase()).size()>0 && termDictionary.get(term.toUpperCase()).get(termDictionary.get(term.toUpperCase()).keySet().toArray()[0]).size()>0)) {
            if (Character.isLowerCase(term.charAt(0))) {
                sum = termDictionary.get(term.toUpperCase()).get(termDictionary.get(term.toUpperCase()).keySet().toArray()[0]).get(0);
            } else {
                sum = termDictionary.get(term.toUpperCase()).get(termDictionary.get(term.toUpperCase()).keySet().toArray()[0]).get(0);
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

    /**
     *this function write the records to a file/
     * @param records line to be writted
     * @param filePath the path of destenation file
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
    public static Map<String, Map<String,ArrayList<Integer>>> getTermDictionary() {
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
    public static void setTermDictionary(Map<String, Map<String, ArrayList<Integer>>> termDictionary) {
        Indexer.termDictionary = termDictionary;
    }
}
