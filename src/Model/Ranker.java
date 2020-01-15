package Model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Ranker {
    private double b_factor;
    private double k_factor;
    private Map<String,Map<String,Integer>> termDocTf;
    private QueryRun query;
    //private static Semaphore mutex = new Semaphore(1);

    /**
     * this function is the constructor of the class
     * @param postingLines
     * @param queryToRank
     * @param SemanticQuery
     */
    public Ranker(Map<String, List<String>> postingLines, List<String> queryToRank,List<String> SemanticQuery){
        termDocTf =new HashMap<>();
        for(String term : queryToRank){
            List<String> tagValues = postingLines.get(term);
            termDocTf.put(term,new HashMap<>());
            for(String str: tagValues){
                String[] postInfo = str.split("\\|");
                termDocTf.get(term).put(postInfo[0],Integer.parseInt(postInfo[1]));
            }
        }
        for(String term : SemanticQuery){
            List<String> tagValues = postingLines.get(term);
            if(!termDocTf.containsKey(term)){
                termDocTf.put(term,new HashMap<>());
                for(String str: tagValues){
                    String[] postInfo = str.split("\\|");
                    termDocTf.get(term).put(postInfo[0],Integer.parseInt(postInfo[1]));
                }
            }
        }

        b_factor = 0.75;
        k_factor = 1.2;

    }
    /**
     * this function is a secondary constructor of the class and get the  b_factor and the k_factor parameters
     *
     * @param k the k_factor
     * @param b the b_factor
     */
    public Ranker(double k, double b){

        if(b>=0 && b<=1 && k>=1.2 && k<=2){
            b_factor = b;
            k_factor = k;
        }
        else{
            b_factor = 0.75;
            k_factor = 1.1;
        }
    }
    /**
     * this function put a score acorrding to the formula
     *
     * @param query the query
     * @param doc the document thar need to be score
     * @param queryRun the queryRun object
     * @return the score
     * @throws InterruptedException
     */
    public double score(List<String> query, String doc, QueryRun queryRun,Map<String,Integer> docLength) throws InterruptedException {

        double totalScore =0;
        int numberOfDocs = ReadFile.getDocs();
        double avrgDocLength = Indexer.getTotalDocLength()/(double)numberOfDocs;
        int iDocLength = docLength.get(doc);
        for(String term: query){
            int queryTF = getQueryTF(query,term);
            int df = queryRun.getPostingLines().get(term).size();
            double idf = getIDF(numberOfDocs,df);
            double numerator = ((double)getTF(term,doc,queryRun))*(k_factor+1);
            double lengthDivision = iDocLength/avrgDocLength;
            double denominator = ((double)getTF(term,doc, queryRun)) +(k_factor*(1-b_factor+b_factor*lengthDivision));
            double termScore = ((double)queryTF)*idf*(numerator/denominator);
            totalScore = totalScore + termScore;
        }

        return totalScore;

    }

    /**
     *this function is a getter
     * @param query
     * @param term
     * @return
     */
    private int getQueryTF(List<String> query, String term) {
        int tf =0;
        for(String str: query){
            if(str.equalsIgnoreCase(term)){
                tf++;
            }
        }
        return tf;
    }

    /*private int getDocLength(String doc) {
        String posting ="";
        HashMap<String, Map <String,Set<String>>> indexer = Indexer.getDocDictionary();
        String path = (String) indexer.get(doc).keySet().toArray()[0];//todo ido change docdic

        // ******* reading first line ********

        try {
            posting =Files.readAllLines(Paths.get(path)).get(0);
        } catch (IOException e) {
            System.out.println(path);
        }

        //******** splitting string by "," *********

        String[] docInfo = posting.split(",");
        return Integer.parseInt(docInfo[2]);
    }*/

    /**
     *this function is a getter
     * @param term
     * @return
     */
    private int getDF(String term) {
        List<String> line = getPostingLine(term);
        return line.size();

    }

    /**
     *this function is a getter
     * @param term
     * @return
     */
    public static List<String> getPostingLine(String term) {
        Pattern TAG_REGEX = Pattern.compile("<(.+?)>", Pattern.DOTALL);
        String posting ="";
        Map<String, Map<String, ArrayList<Integer>>> indexer = Indexer.getTermDictionary();
        String path ="";
        try{
            path = (String)indexer.get(term).keySet().toArray()[0];
        }
        catch(Exception e){
            System.out.println(term);
        }
        int lineNum = indexer.get(term).get(indexer.get(term).keySet().toArray()[0]).get(1);

        // ******* reading specific line ********
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            posting = lines.skip(lineNum).findFirst().get();
            //posting =Files.lines(Paths.get(path)).get(lineNum);
        } catch (IOException e) {
            System.out.println(path);
            System.out.println(lineNum);
        }

        //******** parse strings inside tags <> *********

        posting = posting.substring(posting.indexOf(":")+1);
        List<String> tagValues = new ArrayList<String>();
        Matcher matcher = TAG_REGEX.matcher(posting);
        while (matcher.find()) {
            tagValues.add(matcher.group(1));
        }

        return tagValues;
    }
    /**
     *this function is a getter
     * @param numberOfDocs
     * @param df
     * @return
     */
    private double getIDF(int numberOfDocs, int df) {

        return Math.log((numberOfDocs+1)/((double)df));
    }
    /**
     *this function is a getter
     * @param term
     * @param doc
     * @param queryRun
     * @return
     */
    public int getTF(String term, String doc, QueryRun queryRun){
        /*List<String> tagValues = queryRun.getPostingLines().get(term);
        for(String str: tagValues){
            if(str.startsWith(doc)){
                String[] termInfo = str.split("\\|");
                return Integer.parseInt(termInfo[1]);
            }
        }*/
        if(termDocTf.containsKey(term)){
            if(termDocTf.get(term).containsKey(doc)){
                return termDocTf.get(term).get(doc);
            }
        }

        return 0;
    }
}
