package Model;

import com.medallia.word2vec.Searcher;
import snowball.ext.porterStemmer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class QueryRun implements Runnable {
    private Query query;
    private Map<String, Map<String, Double>> docsRanks;
    private boolean semanticSelected;
    private boolean stem;
    private boolean isDescription;
    private static Semaphore mutex = new Semaphore(1);
    private Searcher semanticSearcher;
    private Map<String,List<String>> postingLines;

    public QueryRun(Query query, Map<String, Map<String, Double>> docsRanks, boolean semanticSelected, boolean stem, boolean isDescription, Searcher semanticSearcher){
        this.query = query;
        this.docsRanks = docsRanks;
        this.semanticSelected = semanticSelected;
        this.stem = stem;
        this.isDescription = isDescription;
        this.semanticSearcher = semanticSearcher;
        postingLines = new HashMap<>();

    }

    @Override
    public void run() {
        runQuery();
    }

    private void runQuery() {
        try {
            Map<String, Double> docRank = getAllRankedDocs(query, semanticSelected, stem, isDescription);
            mutex.acquire();
            docsRanks.put(query.getNumOfQuery(), docRank);
            mutex.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("error running query");
        }
    }

    private Map<String, Double> getAllRankedDocs(Query queriesTokens, boolean semanticSelected, boolean stem, boolean isDescription) throws IOException {
        List<String> queryWithSemantic = new ArrayList<>();

        //intersaction with terms of inverted index
        Set indexedTerms = Indexer.getTermDictionary().keySet();
        //todo change to end of semantic query ;

        Set<String> retrievedDocs = new HashSet<>();

        Map<String, Double> docsRanks = new HashMap<>();

        try {

            int numOfResults = 10;

            if (semanticSelected) {
                getRelevantDocsWithSemantics(queriesTokens, queryWithSemantic, indexedTerms, semanticSearcher, numOfResults, stem, isDescription);
            }

            List<String> queryToRank = queriesTokens.getTokenQuery();
            if (stem) {
                List<String> stemmedQuery = new ArrayList<>();
                for (String term : queryToRank) {
                    stemmedQuery.add(stemTerm(term));
                }
                queryToRank = stemmedQuery;
            }

            if(isDescription){
                getRelevantTermsWithDesc(queryToRank, queriesTokens, stem, isDescription);
            }

            queryToRank.retainAll(indexedTerms);
            getRelevantDocs(queryToRank, retrievedDocs);
            queryWithSemantic.retainAll(indexedTerms);

            docsRanks = rankDocs(retrievedDocs, queryToRank, queryWithSemantic);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return docsRanks;

    }

    private void getRelevantTermsWithDesc(List<String> query, Query queriesTokens, boolean stem, boolean isDescription) {
            ArrayList<String> descSemantic = new ArrayList<>();
            ArrayList<String> descSet = queriesTokens.getTokenDesc();
            for (String desc : descSet) {
                if (stem) {
                    desc = stemTerm(desc);
                }
                descSemantic.add(desc);
                //descSemantic.add(desc.toLowerCase());
                //descSemantic.add(desc.toUpperCase());
            }
            query.addAll(descSemantic);
    }


    private static String stemTerm(String semanticTerm) {
        porterStemmer ps = new porterStemmer();
        ps.setCurrent(semanticTerm);
        ps.stem();
        return ps.getCurrent();
    }

    private Map<String, Double> rankDocs(Set<String> retrievedDocs, List<String> queryToRank, List<String> queryWithSemantic) throws InterruptedException {
        Ranker ranker = new Ranker();
        Map<String, Double> docsRanks = new HashMap<>();

        for (String doc : retrievedDocs) {
            double originalRank = ranker.score(queryToRank, doc,this);
            double newRank = 0.5 * originalRank + 0.5 * ranker.score(queryWithSemantic, doc, this);
            docsRanks.put(doc, newRank);

        }

        return docsRanks;
    }

    private void addDocstoRetrievedDocs(String term, Set<String> retrievedDocs) throws InterruptedException {
        mutex.acquire();
        List<String> postingLine = Ranker.getPostingLine(term);
        mutex.release();
        postingLines.put(term,new ArrayList<>(postingLine));
        for (String str : postingLine) {
            String[] termInfo = str.split("\\|");
            retrievedDocs.add(termInfo[0]);
        }

    }

    private void getRelevantDocs(List<String> queryToRank, Set<String> retrievedDocs) throws InterruptedException {

        for (String queryTerm : queryToRank) {
            addDocstoRetrievedDocs(queryTerm, retrievedDocs);
        }
    }

    private void getRelevantDocsWithSemantics(Query queryToRank, List<String> queryWithSemantic, Set indexedTerms, com.medallia.word2vec.Searcher semanticSearcher, int numOfResults, boolean stem, boolean isDescription) {
        for (String queryTerm : queryToRank.getTokenQuery()) {
            try {

                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(queryTerm, numOfResults);
                for (com.medallia.word2vec.Searcher.Match match : matches) {

                    String semanticTerm = match.match();
                    if (stem) {
                        semanticTerm = stemTerm(semanticTerm);

                    }
                    //todo insert to if queryToRank.getDesc().contains(semanticTerm) and increase numOfResults
                    if ((indexedTerms.contains(semanticTerm) || indexedTerms.contains(semanticTerm.toLowerCase()) || indexedTerms.contains(semanticTerm.toUpperCase())) && !queryTerm.contains(semanticTerm)) {
                        queryWithSemantic.add(semanticTerm.toLowerCase());
                        queryWithSemantic.add(semanticTerm.toUpperCase());
                        queryWithSemantic.add(semanticTerm);
                        //addDocstoRetrievedDocs(semanticTerm,retrievedDocsWithSemantics);
                    }

                }
            } catch (com.medallia.word2vec.Searcher.UnknownWordException e) {
                // TERM NOT KNOWN TO MODEL
            }
        }
    }

    public Map<String, List<String>> getPostingLines() {
        return postingLines;
    }

}
