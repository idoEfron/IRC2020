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
    private static Map<String,List<String>> postingLines =new HashMap<>();;
    private Map<String,Integer> docLength;
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
    public QueryRun(Query query, Map<String, Map<String, Double>> docsRanks, boolean semanticSelected, boolean stem, boolean isDescription, Searcher semanticSearcher, Map<String, Integer> docLength){
        this.query = query;
        this.docsRanks = docsRanks;
        this.semanticSelected = semanticSelected;
        this.stem = stem;
        this.isDescription = isDescription;
        this.semanticSearcher = semanticSearcher;
        this.docLength = docLength;

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
        List<String> descQuery = new ArrayList<>();

        //intersaction with terms of inverted index
        Set indexedTerms = Indexer.getTermDictionary().keySet();
        //todo change to end of semantic query ;

        Set<String> retrievedDocs = new HashSet<>();
        Set<String> semanticDocs = new HashSet<>();
        Set<String> descDocs = new HashSet<>();

        Map<String, Double> docsRanks = new HashMap<>();

        try {

            // get semantic details and docs
            int numOfResults = 6;

            if (semanticSelected) {
                getRelevantDocsWithSemantics(queriesTokens, queryWithSemantic, indexedTerms, semanticSearcher, numOfResults, stem, isDescription);
            }
            queryWithSemantic.retainAll(indexedTerms);
            getRelevantDocs(queryWithSemantic,semanticDocs);

            //get query details and docs
            ArrayList<String> result = new ArrayList<>(queriesTokens.getTokenQuery());
            ArrayList<String> queryToRank = new ArrayList<>();
            for(String qtr: result){
                if(!result.contains(qtr.toLowerCase())){
                    queryToRank.add(qtr.toLowerCase());
                }
                if(!result.contains(qtr.toUpperCase())){
                    queryToRank.add(qtr.toUpperCase());
                }
            }

            //getRelevantTermsWithDesc(descQuery, queriesTokens, stem, isDescription);
            //descQuery.retainAll(indexedTerms);
            //getRelevantDocs(descQuery,descDocs);

            queryToRank.addAll(result);
            if (stem) {
                ArrayList<String> stemmedQuery = new ArrayList<>();
                for (String term : queryToRank) {
                    stemmedQuery.add(stemTerm(term));
                }
                for(String term: stemmedQuery){
                    if(!queryToRank.contains(term)){
                        queryToRank.add(term);
                    }
                }
            }


            queryToRank.retainAll(indexedTerms);
            getRelevantDocs(queryToRank, retrievedDocs);
            semanticDocs.retainAll(retrievedDocs);
            retrievedDocs.addAll(descDocs);

            docsRanks = rankDocs(retrievedDocs, queryToRank, queryWithSemantic,descQuery);

        } catch (Exception e) {
            System.out.println("error ranking query" +queriesTokens.getNumOfQuery());
        }
        return docsRanks;

    }
    /**this function get all the ranked documents in a query with description
     * @param query the query
     * @param queriesTokens the list if token description in a query
     * @param stem if is stemming was selected
     * @param isDescription
     */
    private void getRelevantTermsWithDesc(List<String> query, Query queriesTokens, boolean stem, boolean isDescription) {
        //remove garbage terms
        ArrayList<String> garbage = new ArrayList<>();
        garbage.add("DESCRIPTION");
        garbage.add("DESCRIPTION".toLowerCase());
        garbage.add("available");
        garbage.add("documents");
        garbage.add("document");
        garbage.add("regarding");
        garbage.add("regard");
        garbage.add("find");
        garbage.add("find".toUpperCase());
        garbage.add("provide");
        garbage.add("identify");
        garbage.add("identify".toUpperCase());


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

    /**
     * this function use stemming id a semantic option was selected
     * @param semanticTerm
     * @return the semanticTerm
     */
    private static String stemTerm(String semanticTerm) {
        if(Character.isUpperCase(semanticTerm.charAt(0))){
            String newTerm= new String(semanticTerm.toLowerCase());
            porterStemmer ps = new porterStemmer();
            ps.setCurrent(newTerm);
            ps.stem();
            newTerm = ps.getCurrent().toUpperCase();
            return newTerm;
        }

        String newTerm= semanticTerm;
        porterStemmer ps = new porterStemmer();
        ps.setCurrent(newTerm);
        ps.stem();
        newTerm =ps.getCurrent();
        return newTerm;
    }
    /**+
     * this function ranking all the rekevent document
     * @param retrievedDocs the retrieved Documents of a query
     * @param queryToRank all of the token array of the query
     * @param queryWithSemantic the query with Semantic
     * @return
     * @throws InterruptedException
     */
    private Map<String, Double> rankDocs(Set<String> retrievedDocs, List<String> queryToRank, List<String> queryWithSemantic,List<String> descQuery) throws InterruptedException {
        Ranker ranker = new Ranker(postingLines,queryToRank,queryWithSemantic);
        Map<String, Double> docsRanks = new HashMap<>();

        for (String doc : retrievedDocs) {
            double originalRank = ranker.score(queryToRank, doc,this,this.docLength);
            double newRank = (double)2.1 * originalRank +0.6*ranker.score(descQuery,doc,this,this.docLength) +0.2 * ranker.score(queryWithSemantic, doc, this,this.docLength);
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
        List<String> postingLine = new ArrayList<>();
        mutex.acquire();
        if(!postingLines.containsKey(term)){
            postingLine = Ranker.getPostingLine(term);
            postingLines.put(term,new ArrayList<>(postingLine));
        }
        else{
            postingLine= postingLines.get(term);
        }
        mutex.release();
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
    private void getRelevantDocsWithSemantics(Query queryToRank, List<String> queryWithSemantic, Set indexedTerms, Searcher semanticSearcher, int numOfResults, boolean stem, boolean isDescription) {
        for (String queryTerm : queryToRank.getTokenQuery()) {
            try {

                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(queryTerm, numOfResults);
                for (com.medallia.word2vec.Searcher.Match match : matches) {

                    String semanticTerm = match.match();
                    if (stem) {
                        semanticTerm = stemTerm(semanticTerm);
                    }

                    //todo insert to if queryToRank.getDesc().contains(semanticTerm) and increase numOfResults
                    if ((indexedTerms.contains(semanticTerm) || indexedTerms.contains(semanticTerm.toLowerCase()) || indexedTerms.contains(semanticTerm.toUpperCase()))) {
                        queryWithSemantic.add(semanticTerm.toLowerCase());
                        queryWithSemantic.add(semanticTerm.toUpperCase());
                        //queryWithSemantic.add(semanticTerm);
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
    public static Map<String, List<String>> getPostingLines() {
        return postingLines;
    }

    public Query getQuery() {
        return query;
    }


}
