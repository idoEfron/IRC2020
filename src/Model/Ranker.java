package Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ranker {
    private double b_factor;
    private double k_factor;
    private static Semaphore mutex = new Semaphore(1);

    public Ranker(){

        b_factor = 0.3;
        k_factor = 1.2;

    }

    public Ranker(double k, double b){

        if(b>=0 && b<=1 && k>=1.2 && k<=2){
            b_factor = b;
            k_factor = k;
        }
        else{
            b_factor = 0.75;
            k_factor = 1.2;
        }
    }

    public double score(List<String> query, String doc, QueryRun queryRun) throws InterruptedException {
        mutex.acquire();
        double totalScore =0;
        int numberOfDocs = ReadFile.getDocs();
        double avrgDocLength = Indexer.getTotalDocLength()/(double)numberOfDocs;
        int docLength = getDocLength(doc);
        for(String term: query){

            int df = queryRun.getPostingLines().get(term).size();
            double idf = getIDF(numberOfDocs,df);
            double numerator = getTF(term,doc,queryRun)*(k_factor+1);
            double lengthDivision = docLength/avrgDocLength;
            double denominator = getTF(term,doc, queryRun) +(k_factor*(1-b_factor+b_factor*lengthDivision));
            double termScore = idf*(numerator/denominator);
            totalScore = totalScore + termScore;
        }
        mutex.acquire();

        return totalScore;

    }

    private int getDocLength(String doc) {
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
    }

    private int getDF(String term) {
        List<String> line = getPostingLine(term);
        return line.size();

    }

    public static List<String> getPostingLine(String term){
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
        try {
            posting =Files.readAllLines(Paths.get(path)).get(lineNum);
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

    private double getIDF(int numberOfDocs, int df) {

        return Math.log((numberOfDocs+1)/(df));
    }

    public int getTF(String term, String doc, QueryRun queryRun){
        List<String> tagValues = queryRun.getPostingLines().get(term);
        for(String str: tagValues){
            if(str.startsWith(doc)){
                String[] termInfo = str.split("\\|");
                return Integer.parseInt(termInfo[1]);
            }
        }

        return 0;
    }
}
