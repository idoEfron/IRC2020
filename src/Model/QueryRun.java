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
    private Map<String, List<String>> postingLines;

    /**
     * this function is the constructor of the class
     *
     * @param query            the query
     * @param docsRanks        the map inside map of the docsRanks
     * @param semanticSelected if the semantic option was selected
     * @param stem             if the stammer option was selected
     * @param isDescription
     * @param semanticSearcher
     */
    public QueryRun(Query query, Map<String, Map<String, Double>> docsRanks, boolean semanticSelected, boolean stem, boolean isDescription, Searcher semanticSearcher) {
        this.query = query;
        this.docsRanks = docsRanks;
        this.semanticSelected = semanticSelected;
        this.stem = stem;
        this.isDescription = isDescription;
        this.semanticSearcher = semanticSearcher;
        postingLines = new HashMap<>();

    }

    /**
     * this is the run function
     */
    @Override
    public void run() {
        runQuery();
    }

    /**
     * this function run the query and call getAllRankedDocs to get all the ranked documents in a query
     */
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

    /**
     * this function return all the ranked documents in a query
     *
     * @param queriesTokens    the query
     * @param semanticSelected if semanticSelected was selected
     * @param stem if is stemming was selected
     * @param isDescription
     * @return the RankedDocsin query
     * @throws IOException
     */
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
                getRelevantTermsWithDesc(queryWithSemantic, queriesTokens, stem, isDescription);
            }

            ArrayList<String> result = new ArrayList<>(queriesTokens.getTokenQuery());
            ArrayList<String> queryToRank = new ArrayList<>();
            for (String qtr : result) {
                if (!result.contains(qtr.toLowerCase())) {
                    queryToRank.add(qtr.toLowerCase());
                }
                if (!result.contains(qtr.toUpperCase())) {
                    queryToRank.add(qtr.toUpperCase());
                }
            }

            queryToRank.addAll(result);
            if (stem) {
                ArrayList<String> stemmedQuery = new ArrayList<>();
                for (String term : queryToRank) {
                    stemmedQuery.add(stemTerm(term));
                }
                queryToRank = stemmedQuery;
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

     /**this function get all the ranked documents in a query with description
     * @param query the query
     * @param queriesTokens the list if token description in a query
     * @param stem if is stemming was selected
     * @param isDescription
     */
    private void getRelevantTermsWithDesc(List<String> queriesTokens, Query query , boolean stem, boolean isDescription) {
        //remove garbage terms
        ArrayList<String> garbage = new ArrayList<>();
        garbage.add("DESCRIPTION");
        garbage.add("DESCRIPTION".toLowerCase());
        garbage.add("available");
        garbage.add("documents");
        garbage.add("document");
        garbage.add("discuss");
        garbage.add("discussing");
        garbage.add("regarding");
        garbage.add("regard");
        garbage.add("find");
        garbage.add("find".toUpperCase());
        garbage.add("provide");
        garbage.add("identify");
        garbage.add("identify".toUpperCase());


        ArrayList<String> descSemantic = new ArrayList<>();
        ArrayList<String> descSet = query.getTokenDesc();
        for (String desc : descSet) {
            if (!garbage.contains(desc)) {
                if (stem) {
                    desc = stemTerm(desc);
                }
            }
            descSemantic.add(desc);
            //descSemantic.add(desc.toLowerCase());
            //descSemantic.add(desc.toUpperCase());
        }
        queriesTokens.addAll(descSemantic);
    }

    /**
     * this function use stemming id a semantic option was selected
     * @param semanticTerm
     * @return the semanticTerm
     */
    private static String stemTerm(String semanticTerm) {
        porterStemmer ps = new porterStemmer();
        ps.setCurrent(semanticTerm);
        ps.stem();
        return ps.getCurrent();
    }

    /**+
     * this function ranking all the rekevent document
     * @param retrievedDocs the retrieved Documents of a query
     * @param queryToRank all of the token array of the query
     * @param queryWithSemantic the query with Semantic
     * @return
     * @throws InterruptedException
     */
    private Map<String, Double> rankDocs(Set<String> retrievedDocs, List<String> queryToRank, List<String> queryWithSemantic) throws InterruptedException {
        Ranker ranker = new Ranker();
        Map<String, Double> docsRanks = new HashMap<>();

        for (String doc : retrievedDocs) {
            double originalRank = ranker.score(queryToRank, doc, this);
            double newRank = 0.5 * originalRank + 0.5 * ranker.score(queryWithSemantic, doc, this);
            docsRanks.put(doc, newRank);

        }

        return docsRanks;
    }

    /**
     * this function add the return documents to the retrievedDocs
     * @param term the term
     * @param retrievedDocs the retrievedDocs data structure
     * @throws InterruptedException
     */
    private void addDocstoRetrievedDocs(String term, Set<String> retrievedDocs) throws InterruptedException {
        mutex.acquire();
        List<String> postingLine = Ranker.getPostingLine(term);
        mutex.release();
        postingLines.put(term, new ArrayList<>(postingLine));
        for (String str : postingLine) {
            String[] termInfo = str.split("\\|");
            retrievedDocs.add(termInfo[0]);
        }

    }

    /**
     * this function get all the relevant document
     * @param queryToRank
     * @param retrievedDocs
     * @throws InterruptedException
     */
    private void getRelevantDocs(List<String> queryToRank, Set<String> retrievedDocs) throws InterruptedException {

        for (String queryTerm : queryToRank) {
            addDocstoRetrievedDocs(queryTerm, retrievedDocs);
        }
    }

    /**
     * this function get all the relevant document with semantics
     * @param queryToRank the query
     * @param queryWithSemantic the tokens of the query with Semantic
     * @param indexedTerms
     * @param semanticSearcher the semanticSearcher
     * @param numOfResults
     * @param stem if stammer option was selected
     * @param isDescription
     */
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

    /**
     * this functuin get the posting line for each term
     * @return the posting line map
     */
    public Map<String, List<String>> getPostingLines() {
        return postingLines;
    }

}
