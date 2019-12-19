package ViewModel;

import Model.*;
import Model.Merge;
import Model.ReadFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class viewModel {
    public int[] start(boolean stem, String postPath, String corpusPath) throws IOException {

        File[] files1 = null;
        File folder = new File(corpusPath);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        File subFolderTerms = null;
        boolean corpus;
        boolean subFolder1;
        boolean subFolder2;
        if (stem) {
            File directory = new File(postPath + "/StemmedCorpus");
            corpus = directory.mkdir();
            subFolderTerms = new File(postPath + "/StemmedCorpus/Terms");
            subFolder1 = subFolderTerms.mkdir();
            File subFolderDocs = new File(postPath + "/StemmedCorpus/Docs");
            subFolder2 = subFolderDocs.mkdir();
            for (char i = 'a'; i <= 'z'; i++) {
                File Tfolder = new File(postPath + "/StemmedCorpus/Terms/" + i);
                Tfolder.mkdir();
                File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
                merged.createNewFile();
            }
            File Sfolder = new File(postPath + "/StemmedCorpus/Terms/special");
            Sfolder.mkdir();
            File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
            merged.createNewFile();
            new File(subFolderDocs.getPath() + "/docDictionary").mkdir();
            File mergedDoc = new File(subFolderDocs.getPath() + "/docDictionary", "docDictionary" + "_merged.txt");
            mergedDoc.createNewFile();
        } else {
            File directory = new File(postPath + "/Corpus");
            corpus = directory.mkdir();
            subFolderTerms = new File(postPath + "/Corpus/Terms");
            subFolder1 = subFolderTerms.mkdir();
            File subFolderDocs = new File(postPath + "/Corpus/Docs");
            subFolder2 = subFolderDocs.mkdir();
            for (char i = 'a'; i <= 'z'; i++) {
                File Tfolder = new File(postPath + "/Corpus/Terms/" + i);
                Tfolder.mkdir();
                File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
                merged.createNewFile();
            }
            File Sfolder = new File(postPath + "/Corpus/Terms/special");
            Sfolder.mkdir();
            File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
            merged.createNewFile();
            new File(subFolderDocs.getPath() + "/docDictionary").mkdir();
            File mergedDoc = new File(subFolderDocs.getPath() + "/docDictionary", "docDictionary" + "_merged.txt");
            mergedDoc.createNewFile();
        }
        if (!corpus || !subFolder1 || !subFolder2) {
            int[] error = {0};
            return error;
        }


        if (folder.isDirectory()) {
            File[] listOfSubFolders = folder.listFiles();
            List<File> files = new ArrayList<>();
            for (File SubFolder : listOfSubFolders) {
                if (SubFolder.isDirectory()) {
                    files.add(SubFolder);
                    if (files.size() == 5) {
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
        executor = Executors.newFixedThreadPool(5);
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
        Map<String,Map<String,Integer>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(Indexer.getTermDictionary());
        index.setTermDictionary(map);
        int[] corpusInfo = new int[2];
        corpusInfo[0] = index.getNumberOfTerms();
        corpusInfo[1] = index.getNumberOrDocuments();
        return corpusInfo;
    }

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

    public String displayDictionary(boolean selected, String postPath) throws IOException {
        String dictionary = "";
        Indexer index = new Indexer(selected, postPath);
        Set<String> termsKey = index.getTermDictionary().keySet();
        if (termsKey.size() > 0) {
            for (String term : termsKey) {
                String currTerm = term;
                currTerm = "The TF for : " + currTerm + " -> " + index.getTermDictionary().get(term).get(index.getTermDictionary().get(term).keySet().toArray()[0]);
                dictionary = dictionary + currTerm + "\n";
            }
            return dictionary;
        } else {
            return "";
        }
    }

    public void loadDictionary(boolean selected, String postPath) throws IOException {
        File file = new File(postPath + "/termDictionary.txt");
        Indexer index = new Indexer(selected, postPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        Map<String, Map<String, Integer>> termDictionary = new HashMap<>();
        index.clearMap();
        while ((st = br.readLine()) != null) {
            String[] term = st.split(",");
            if(term.length==3) {
                termDictionary.put(term[0], new HashMap<>());
                termDictionary.get(term[0]).put(term[1], Integer.parseInt(term[2]));
            }
        }
        index.setTermDictionary(termDictionary);
    }
}
