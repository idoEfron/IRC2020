package ViewModel;

import Model.*;
import Model.Merge;
import Model.ReadFile;
import com.medallia.word2vec.Word2VecModel;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class viewModel {
    private File subFolderTerms = null;

    /**
     * this function is the starting function of the program processes
     *
     * @param stem       if stemming is checked
     * @param postPath   the posting files path
     * @param corpusPath the corpus path
     * @return int array of the alert details
     * @throws IOException
     */

    public int[] start(boolean stem, String postPath, String corpusPath) throws IOException {

        File[] files1 = null;
        File folder = new File(corpusPath);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        if (stem) {
            createFolders(postPath, "StemmedCorpus");
        } else {
            createFolders(postPath, "Corpus");
        }


        if (folder.isDirectory()) {
            File[] listOfSubFolders = folder.listFiles();
            List<File> files = new ArrayList<>();
            for (File SubFolder : listOfSubFolders) {
                if (SubFolder.isDirectory()) {
                    files.add(SubFolder);
                    if (files.size() == 20) {
                        ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stem, postPath), stem, corpusPath);
                        executor.execute(new Thread(read));
                        files.clear();
                    }
                }
            }
            if (!files.isEmpty()) {
                ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stem, postPath), stem, corpusPath);
                executor.execute(new Thread(read));
                files.clear();
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        }

        executor = Executors.newFixedThreadPool(4);
        for (File file : subFolderTerms.listFiles()) {
            if (file.isDirectory()) {
                Merge merge = new Merge(file.listFiles());
                executor.execute(new Thread(merge));
            }
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }


        Indexer index = new Indexer(stem, postPath);

        File file = new File( postPath + "/termDictionary.txt" );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        for (Map.Entry<String,Map<String,ArrayList<Integer>>> records : Indexer.getTermDictionary().entrySet()) {
            try{
                Integer tf = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(0);
                Integer line = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(1);
                writer.write(records.getKey()+">"+records.getValue().keySet().toArray()[0]+">"+(records.getValue().get(records.getValue().keySet().toArray()[0])).get(0)+">"+(records.getValue().get(records.getValue().keySet().toArray()[0])).get(1)+"\n");
            }
            catch(Exception e){
                System.out.println(records.getKey());
            }
        }
        writer.flush();
        writer.close();

        int[] corpusInfo = new int[2];
        corpusInfo[0] = index.getNumberOfTerms();
        corpusInfo[1] = ReadFile.getDocs();
        ReadFile.setDocs(0);
        return corpusInfo;
    }

    /**
     * this function deletes all files and directories in given directory/file
     *
     * @param file the file in which we want to recursively delete all files and directories
     */
    public void delete(File file) {
        String[] lists = file.list();
        if (lists.length > 0) {
            for (String s : lists) {
                File currentFile = new File(file.getPath(), s);
                if (currentFile.isDirectory()) {
                    delete(currentFile);
                }
                currentFile.delete();
            }
        }
    }

    /**
     * this function creates all needed folders of the program
     * @param postPath posting files path
     * @param folder   folder name depending on with stemming or without
     * @throws IOException
     */

    private void createFolders(String postPath, String folder) throws IOException {
        File directory = new File(postPath + "/" + folder);
        directory.mkdir();
        subFolderTerms = new File(postPath + "/" + folder + "/Terms");
        subFolderTerms.mkdir();
        File subFolderDocs = new File(postPath + "/" + folder + "/Docs");
        subFolderDocs.mkdir();
        for (char i = 'a'; i <= 'z'; i++) {
            File Tfolder = new File(postPath + "/" + folder + "/Terms/" + i);
            Tfolder.mkdir();
            File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
            merged.createNewFile();
        }
        File Sfolder = new File(subFolderTerms.getPath() + "/special");
        Sfolder.mkdir();
        File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
        merged.createNewFile();
        //File mergedDoc = new File(subFolderDocs.getPath() + "/docDictionary", "docDictionary" + "_merged.txt");
        //mergedDoc.createNewFile();
    }

    /**
     * this function display the dictionary generated from the program
     *
     * @param selected stemming selected/not selected
     * @param postPath posting files path
     * @return the String to be displayed
     * @throws IOException
     */

    public LinkedList displayDictionary(boolean selected, String postPath) throws IOException {
        //String dictionary = "";
        Indexer index = new Indexer(selected, postPath);
        Set<String> termsKey = index.getTermDictionary().keySet();
        LinkedList<String> dictionary = new LinkedList<>();
        if (termsKey.size() > 0) {
            for (String term : termsKey) {
                String currTerm = term;
                dictionary.add("The TF for : " + currTerm + " -> " + index.getTermDictionary().get(term).get(index.getTermDictionary().get(term).keySet().toArray()[0]));
                //dictionary = dictionary + currTerm + "\n";
            }
            return dictionary;
        } else {
            return null;
        }
    }

    /**
     * this function loads dictionary from the hard disk to RAM
     *
     * @param selected stemming selected/not selected
     * @param postPath posting file path
     * @throws IOException
     */

    public void loadDictionary(boolean selected, String postPath) throws IOException {
        File file = new File(postPath + "/termDictionary.txt");
        Indexer index = new Indexer(selected, postPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        Map<String, Map<String, ArrayList<Integer>>> termDictionary = new TreeMap<>();
        index.clearMap();
        while ((st = br.readLine()) != null) {
            String[] term = st.split(">");
            if (term.length == 3) {
                termDictionary.put(term[0], new HashMap<>());
                termDictionary.get(term[0]).put(term[1], new ArrayList<>());
                termDictionary.get(term[0]).get(term[1]).set(0, Integer.parseInt(term[2]));
            }
        }
        index.setTermDictionary(termDictionary);
    }

    public List<String> startQuery(String path, String stopWordsPath, boolean stem, boolean semanticSelected) throws IOException, ParseException, InterruptedException {
        Searcher searcher = new Searcher(path, stopWordsPath, stem);
        searcher.readQuery();
        List<String>queryToRank = searcher.getQueriesTokens();
        return queryToRank;//todo change to documents
    }

    public  List<String> startSingleQuery(String query, String stopWordsPath, boolean stem, boolean semanticSelected) throws IOException, ParseException, InterruptedException {
        Searcher searcher = new Searcher(query, stopWordsPath, stem);
        searcher.startSingleQuery();

        List<String> queryToRank = searcher.getQueriesTokens();
        Set indexedTerms = Indexer.getTermDictionary().keySet();
        queryToRank.retainAll(indexedTerms);
        Set<String> retrievedDocs = new HashSet<>();
        Set<String> retrievedDocsWithSemantics = new HashSet<>();
        List<String> queryWithSemantic = new ArrayList<>();
        if (semanticSelected) {
            for (String queryTerm : queryToRank) {
                try {
                    Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
                    com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
                    int numOfResults = 20;
                    List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(queryTerm, numOfResults);
                    for (com.medallia.word2vec.Searcher.Match match : matches) {
                        String sematicTerm =match.match();
                        if(indexedTerms.contains(sematicTerm)){
                            queryWithSemantic.add(match.match());
                            addDocstoRetrievedDocs(sematicTerm,retrievedDocsWithSemantics);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (com.medallia.word2vec.Searcher.UnknownWordException e) {
                    // TERM NOT KNOWN TO MODEL
                }
            }
        }

        for (String queryTerm : queryToRank) {
            addDocstoRetrievedDocs(queryTerm,retrievedDocs);
        }
        return null;//todo change
    }
    private Map<String,Integer> topFifty(Map<String,Integer> docRanked){
        //Map<String,Integer> topFifty = new HashMap<>();
        if(docRanked.size()>50) {
            int numberOfdocs = 0;
            Map<String,Integer> topFifty = new HashMap<>();
            while (numberOfdocs!=50) {
                //int max = entitiesPerDoc.get(0);
                Set<String> str = docRanked.keySet();
                String [] strArr = new String[docRanked.keySet().size()];
                strArr = str.toArray(strArr);
                int max = docRanked.get(strArr[0]);
                String maxString  =strArr[0];
                for (int k = 1; k < strArr.length; k++) {
                    if (docRanked.get(strArr[k])>max) {
                        max = docRanked.get(strArr[k]);
                        maxString = strArr[k];
                    }
                }
                docRanked.remove(maxString);
                topFifty.put(maxString,max);
                numberOfdocs++;
            }
            return topFifty;
        }else if(docRanked.keySet().size()>0){
            return docRanked;
        }
        return null;
    }
    private void addDocstoRetrievedDocs(String term, Set<String> retrievedDocs) {
        List<String> postingLine = Ranker.getPostingLine(term);
        for (String str : postingLine) {
            String[] termInfo = str.split("\\|");
            retrievedDocs.add(termInfo[0]);
        }
    }
}
