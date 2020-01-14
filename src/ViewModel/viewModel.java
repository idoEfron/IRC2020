package ViewModel;

import Model.*;
import Model.Merge;
import Model.ReadFile;
import com.medallia.word2vec.Word2VecModel;
import snowball.ext.porterStemmer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class viewModel {
    private File subFolderTerms = null;
    private List<String> documentInQuery;
    private static String postPath;
    private Map<String, Map<String, Double>> docsRanks;
    private String corPath;
    private Map<String, Map<String, Double>> topFifty;

    public viewModel() {
        documentInQuery = new LinkedList<>();
    }

    public static String getPostPath() {
        return postPath;
    }

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

        this.postPath = postPath;
        ReadFile.setDocs(0);
        Indexer.getTermDictionary().clear();
        Indexer.getDocDictionary().clear();
        Indexer.setTotalDocLength(0);
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
                Merge merge = new Merge(file.listFiles(), false);
                executor.execute(new Thread(merge));
            }
        }

        executor.shutdown();

        while (!executor.isTerminated()) {
        }

        File stopWords = new File(corpusPath+"/"+"stop_words.txt");
        File dest = new File(postPath+"/"+stopWords.getName());
        dest.createNewFile();
        Files.copy(stopWords.toPath(),dest.toPath() , StandardCopyOption.REPLACE_EXISTING);

        File fileDocs = new File(postPath + "/docsEnts");
        File[] fileArr = fileDocs.listFiles();
        Merge merge = new Merge(fileDocs.listFiles(), true);
        merge.run();

        File docEntsMerge = new File(postPath + "/docsEnts/docsEnts_merged.txt");
        uploadMap(postPath, docEntsMerge);

        Indexer index = new Indexer(stem, postPath);

        File file = new File(postPath + "/termDictionary.txt");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        for (Map.Entry<String, Map<String, ArrayList<Integer>>> records : Indexer.getTermDictionary().entrySet()) {
            try {
                Integer tf = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(0);
                Integer line = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(1);
                writer.write(records.getKey() + ">" + records.getValue().keySet().toArray()[0] + ">" + (records.getValue().get(records.getValue().keySet().toArray()[0])).get(0) + ">" + (records.getValue().get(records.getValue().keySet().toArray()[0])).get(1) + "\n");
            } catch (Exception e) {
                System.out.println(records.getKey());
            }
        }


        File fileDoc = new File(postPath + "/docDictionary.txt");
        fileDoc.createNewFile();
        FileWriter writerDoc = new FileWriter(fileDoc);
        for (String doc : Indexer.getDocDictionary().keySet()) {
            String doctxt = doc + ">" + (String) Indexer.getDocDictionary().get(doc).keySet().toArray()[0] + ">" + Indexer.getDocDictionary().get(doc).values();
            writerDoc.write(doctxt + "\n");
        }
        writer.flush();
        writer.close();
        writerDoc.flush();
        writerDoc.close();

        int[] corpusInfo = new int[2];
        corpusInfo[0] = index.getNumberOfTerms();
        corpusInfo[1] = ReadFile.getDocs();

        return corpusInfo;
    }

    private void uploadMap(String posting, File docEntities) throws IOException {
        Parser.cleanEntities();
        Indexer.clearDocdic();
        if (docEntities.exists()) {
            List<String> doctxt = Files.readAllLines(docEntities.toPath(), StandardCharsets.UTF_8);
            //docEntities.delete();
            HashMap<String, Map<String, String>> hashDocEnt = new HashMap<>();
            for (int i = 0; i < doctxt.size(); i++) {
                String[] strArr = doctxt.get(i).split(" : ");
                hashDocEnt.put(strArr[0], new HashMap<>());
                String map = strArr[1].replaceAll("\\{", "");
                map = map.replaceAll("}", "");
                if (map.contains(",")) {
                    String[] split = map.split(", ");
                    for (String s : split) {
                        try {
                            if (s.contains("=")) {
                                String[] value = s.split("=");
                                hashDocEnt.get(strArr[0]).put(value[0], value[1]);
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println(strArr[0]);
                        }
                    }
                }
            }
            HashMap<String, Map<String, Set<String>>> docDictionary = new HashMap<>();
            if (hashDocEnt.size() > 0) {
                for (String s : hashDocEnt.keySet()) {
                    hashDocEnt.get(s).keySet().retainAll(Indexer.getTermDictionary().keySet());
                    docDictionary.put(s, new HashMap<>());
                    // selectTopFive(s,postingPath,hashDocEnt.get(s));
                    docDictionary.put(s, selectTopFive(s, posting, hashDocEnt.get(s)));
                }
                Indexer.setDocDictionary(docDictionary);
            }

        }
    }

    public Map<String, Set<String>> selectTopFive(String s, String postingPath, Map<String, String> hashDocEnt) {
        //Map<String, String> copyHashDocEnt = new HashMap<>(hashDocEnt);
        Set<String> topFive = new HashSet<>();
        Map<String, Set<String>> topFiveEntitiesDocs = new HashMap<>();
        if (hashDocEnt.size() > 5) {
            int numberOfEntities = 0;
            while (numberOfEntities != 5) {
                //int max = entitiesPerDoc.get(0);
                Set<String> str = hashDocEnt.keySet();
                String[] strArr = new String[hashDocEnt.keySet().size()];
                strArr = str.toArray(strArr);
                int max = Integer.parseInt(hashDocEnt.get(strArr[0]));
                String maxString = strArr[0];
                for (int k = 1; k < strArr.length; k++) {
                    if (Integer.parseInt(hashDocEnt.get(strArr[k])) > max) {
                        max = Integer.parseInt(hashDocEnt.get(strArr[k]));
                        maxString = strArr[k];
                    }
                }
                hashDocEnt.remove(maxString);
                topFive.add("the ranking of the entity :" + maxString + " is " + max);
                numberOfEntities++;
            }
            topFiveEntitiesDocs.put(corPath + "/Docs/" + s + ".txt", topFive);
        } else if (hashDocEnt.size() >= 0) {
            for (String str :hashDocEnt.keySet()){
                topFive.add("the ranking of the entity :" + str + " is " + hashDocEnt.get(str));
            }
            topFiveEntitiesDocs.put(corPath + "/Docs/" + s + ".txt", topFive);
        }
        return topFiveEntitiesDocs;
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
     *
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
        File subEntDocs = new File(postPath + "/docsEnts");
        subEntDocs.mkdir();
        File mergeEntDocs = new File(subEntDocs.getPath() + "/docsEnts_merged.txt");
        mergeEntDocs.createNewFile();
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
        corPath = directory.getAbsolutePath();
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

    public LinkedList<String> displayDictionary(boolean selected, String postPath) throws IOException {
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
        this.postPath = postPath;
        File file = new File(postPath + "/termDictionary.txt");
        Indexer index = new Indexer(selected, postPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        Map<String, Map<String, ArrayList<Integer>>> termDictionary = new TreeMap<>();
        index.clearMap();
        while ((st = br.readLine()) != null) {
            String[] term = st.split(">");
            if (term.length == 4) {
                termDictionary.put(term[0], new HashMap<>());
                String word = term[1].substring(term[1].lastIndexOf('/')+1,term[1].lastIndexOf("_"));
                String path = "";
                if(selected){
                    path = postPath+"/StemmedCorpus/Terms/"+word+term[1].substring(term[1].lastIndexOf('/'));
                }
                else{
                    path = postPath+"/Corpus/Terms/"+word+term[1].substring(term[1].lastIndexOf('/'));
                }
                termDictionary.get(term[0]).put(path, new ArrayList<>());
                termDictionary.get(term[0]).get(path).add(0, Integer.parseInt(term[2]));
                termDictionary.get(term[0]).get(path).add(1, Integer.parseInt(term[3]));
            }
        }
        index.setTermDictionary(termDictionary);
        br.close();
        File docFile = new File(postPath + "/docDictionary.txt");
        BufferedReader brDoc = new BufferedReader(new FileReader(docFile));
        String strDoc;
        HashMap<String, Map<String, Set<String>>> docDictionary = new HashMap<>();
        while ((strDoc = brDoc.readLine()) != null) {
            String[] term = strDoc.split(">");
            String word = term[1].substring(term[1].lastIndexOf('/')+1);
            String path = "";
            if(selected){
                path = postPath+"/StemmedCorpus/Docs/"+word;
            }
            else{
                path = postPath+"/Corpus/Docs/"+word;
            }
            docDictionary.put(term[0], new HashMap<>());
            docDictionary.get(term[0]).put(path, new HashSet<>());
            String set = term[2];
            set = set.replaceAll("\\[", "");
            set = set.replaceAll("]", "");
            String[] setArr = set.split(",");
            for (int i = 0; i < setArr.length; i++) {
                docDictionary.get(term[0]).get(path).add(setArr[i]);
            }
        }
        index.setDocDictionary(docDictionary);
        brDoc.close();
        File corpusInfo = new File(postPath + "/corpusInfo.txt");
        BufferedReader buffCor = new BufferedReader(new FileReader(corpusInfo));
        String strCor;
        if ((strCor = buffCor.readLine()) != null) {
            String[] strArr = strCor.split(">");
            if (strArr.length == 2) {
                ReadFile.setDocs(Integer.parseInt(strArr[0]));
                Indexer.setTotalDocLength(Double.parseDouble(strArr[1]));
            }
        }
        buffCor.close();
    }

    public LinkedList<String> startQuery(String path, String stopWordsPath, boolean stem, boolean semanticSelected, boolean isDescription) throws IOException, ParseException, InterruptedException {
        Searcher searcher = new Searcher(path, stopWordsPath, stem, isDescription);
        List<Query> queryList = searcher.readQuery();
        Map<String, Map<String, Double>> docsRanks = new HashMap<>();

        Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
        com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (Query query : queryList) {
            QueryRun queryRun = new QueryRun(query, docsRanks, semanticSelected, stem, isDescription, semanticSearcher);
            executor.execute(new Thread(queryRun));
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        //writeToResultFile(docsRanks);

        this.docsRanks = docsRanks;

        return displayQueries(docsRanks);
    }

    private LinkedList<String> displayQueries(Map<String, Map<String, Double>> docsRanks) {
        this.topFifty = new TreeMap<>();
        LinkedList<String> display = new LinkedList<>();
        int num = 0;
        for (String s : docsRanks.keySet()) {
            String str = "";
            display.add("For query number " + s + " the most fifty or less documents are:" + "\n");
            this.topFifty.put(s, topFifty(docsRanks.get(s)));
            Set<String> topfifty = topFifty(docsRanks.get(s)).keySet();
            for (String docStr : topfifty) {
                documentInQuery.add(docStr);
                str = str + "," + docStr;
                num++;
                if (str.split(",").length % 10 == 0) {
                    display.add(str);
                    str = "";
                }
            }
            display.add(str);
        }
        display.addFirst("Total number of queries are :" + docsRanks.keySet().size() + "\n"
                + "Total document returned for all the queries are :" + num);
        return display;
    }

    public LinkedList<String> startSingleQuery(String query, String stopWordsPath, boolean stem, boolean semanticSelected, boolean isDescription) throws IOException, ParseException, InterruptedException {

        Searcher searcher = new Searcher(query, stopWordsPath, stem, isDescription);
        Query singleQuery = searcher.startSingleQuery();

        Map<String, Map<String, Double>> docsRanks = new HashMap<>();
        Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
        com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
        QueryRun queryRun = new QueryRun(singleQuery, docsRanks, semanticSelected, stem, isDescription, semanticSearcher);
        queryRun.run();
        //docsRanks.put(singleQuery.getNumOfQuery(), getAllRankedDocs(singleQuery, semanticSelected, stem, isDescription));

        //writeToResultFile(docsRanks);
        this.docsRanks = docsRanks;

        return displayQueries(docsRanks);
    }

    public void writeToResultFile(String path) throws IOException {
        if (topFifty.size() > 0) {
            File file = new File(path + "/results.txt");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            for (Map.Entry<String, Map<String, Double>> entry : topFifty.entrySet()) {
                for (Map.Entry<String, Double> docEntry : entry.getValue().entrySet()) {
                    writer.write(entry.getKey() + " " + "0" + " " + docEntry.getKey() + " " + docEntry.getValue() + " " + "42.38" + " " + "i&i" + '\n');
                }
            }
            writer.flush();
            writer.close();
        }
    }

    public Map<String, Map<String, Double>> getDocsRanks() {
        return docsRanks;
    }

    public List<String> getDocumentInQuery() {
        return documentInQuery;
    }

    private Map<String, Double> topFifty(Map<String, Double> docRanked) {
        Map<String, Double> docRankCopy = new HashMap<>(docRanked);
        if (docRanked.size() > 50) {
            int numberOfdocs = 0;
            Map<String, Double> topFifty = new HashMap<>();
            while (numberOfdocs != 50) {
                //int max = entitiesPerDoc.get(0);
                Set<String> str = docRankCopy.keySet();
                String[] strArr = new String[docRankCopy.keySet().size()];
                strArr = str.toArray(strArr);
                double max = docRankCopy.get(strArr[0]);
                String maxString = strArr[0];
                for (int k = 1; k < strArr.length; k++) {
                    if (docRankCopy.get(strArr[k]) > max) {
                        max = docRankCopy.get(strArr[k]);
                        maxString = strArr[k];
                    }
                }
                docRankCopy.remove(maxString);
                topFifty.put(maxString, max);
                numberOfdocs++;
            }
            return topFifty;
        } else if (docRankCopy.keySet().size() >= 0) {
            return docRanked;
        }
        return null;
    }
}
