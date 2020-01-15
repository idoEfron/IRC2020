package Model;

import com.medallia.word2vec.Word2VecModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Searcher {
    private Parser parser;
    private ReadFile readFile;
    private Indexer indexer;
    private String query;
    private boolean description;
    private boolean stem;
    private ArrayList<String> queriesTokens;
    private Map<String, Integer> docLength;

    /**
     * this function is the constructor of the class
     *
     * @param query        the query
     * @param stopWordPath the stop word path
     * @param stem         id stammer option was selected
     * @param description  if description was selected
     * @param docLength
     * @throws IOException
     * @throws ParseException
     */
    public Searcher(String query, String stopWordPath, Boolean stem, boolean description, Map<String, Integer> docLength) throws IOException, ParseException {
        indexer = new Indexer(stem, stopWordPath);
        readFile = new ReadFile(null, indexer, stem, stopWordPath);
        parser = new Parser(false, readFile, stopWordPath, indexer, true);
        this.query = query;
        this.description = description;
        this.stem = stem;
        queriesTokens = new ArrayList<>();
        this.docLength = docLength;
    }

    /**
     * this function is a getter
     *
     * @return queriesTokens
     */
    public ArrayList<String> getQueriesTokens() {
        return queriesTokens;
    }

    /**
     * this function read akk the query fields and value
     *
     * @return the list of object query
     * @throws ParseException
     * @throws InterruptedException
     * @throws IOException
     */
    public List<Query> readQuery() throws ParseException, InterruptedException, IOException {
        List<Query> listQueries = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(query));
            String line = reader.readLine();
            while (line != null) {
                if (line.equals("<top>")) {
                    String numOfQuery = "";
                    String title = "";
                    String description = "";
                    String narrative = "";
                    while (line != null && !line.equals("</top>")) {
                        if (line.contains("<num>")) {
                            line = line.replaceAll("\\<.*?\\>", "");
                            numOfQuery = line.substring(line.indexOf(":") + 1);
                        }
                        if (line.contains("<title>")) {
                            line = line.replaceAll("\\<.*?\\>", "");
                            title = line;
                        }
                        if (line.contains("<desc>")) {
                            line = line.replaceAll("\\<.*?\\>", "");
                            description = line;
                            line = reader.readLine();
                            while (line != null && !line.contains("<narr>") && !line.equals("</top>")) {
                                description = description + line;
                                line = reader.readLine();
                            }
                        }
                        if (line.contains("<narr>")) {
                            line = line.replaceAll("\\<.*?\\>", "");
                            narrative = line;
                            line = reader.readLine();
                            while (line != null && !line.equals("</top>")) {
                                narrative = narrative + line;
                                line = reader.readLine();
                            }
                        }
                        if (line.equals("</top>")) {
                            break;
                        }
                        line = reader.readLine();
                    }
                    Query query = new Query(numOfQuery, title, description, narrative);
                    listQueries.add(query);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < listQueries.size(); i++) {
            Query query = listQueries.get(i);
            queriesTokens = new ArrayList<>();
            String title = "<TEXT>" + query.getTitle() + "</TEXT>";

            ArrayList<String> temp = new ArrayList<>();
            temp.add(title);
            parser.parseDocs(temp);
            queriesTokens = new ArrayList<>(parser.getQueryArray());
            query.setTokenQuery(queriesTokens);
            parser.getQueryArray().clear();

            ArrayList<String> descArray = new ArrayList<>();
            String desc = "";
            desc = "<TEXT>" + query.getDescription() + "</TEXT>";
            descArray.add(desc);
            parser.parseDocs(descArray);
            query.setTokenDesc(new ArrayList<>(parser.getQueryArray()));

        }
        return listQueries;
    }

    /**
     * this function start a single query and analyze it
     * @return thw query
     * @throws ParseException
     * @throws InterruptedException
     * @throws IOException
     */
    public Query startSingleQuery() throws ParseException, InterruptedException, IOException {
        String q = query;
        Query queryParse = null;
        Scanner scanner = new Scanner(query);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("<top>")) {
                String numOfQuery = "";
                String title = "";
                String description = "";
                String narrative = "";
                while (line != null && !line.equals("</top>")) {
                    if (line.contains("<num>")) {
                        line = line.replaceAll("\\<.*?\\>", "");
                        numOfQuery = line.substring(line.indexOf(":") + 1);
                    }
                    if (line.contains("<title>")) {
                        line = line.replaceAll("\\<.*?\\>", "");
                        title = line;
                    }
                    if (line.contains("<desc>")) {
                        line = line.replaceAll("\\<.*?\\>", "");
                        description = line;
                        line = scanner.nextLine();
                        while (line != null && !line.contains("<narr>") && !line.equals("</top>")) {
                            description = description + line;
                            line = scanner.nextLine();
                        }
                    }
                    if (line.contains("<narr>")) {
                        line = line.replaceAll("\\<.*?\\>", "");
                        narrative = line;
                        line = scanner.nextLine();
                        while (line != null && !line.equals("</top>")) {
                            narrative = narrative + line;
                            line = scanner.nextLine();
                        }
                    }
                    if (line.equals("</top>")) {
                        break;
                    }
                    line = scanner.nextLine();
                }
                queryParse = new Query(numOfQuery, title, description, narrative);
            }
        }
        scanner.close();
        String title = "<TEXT>" + queryParse.getTitle() + "</TEXT>";
        ArrayList<String> temp = new ArrayList<>();
        temp.add(title);
        parser.parseDocs(temp);
        queriesTokens = new ArrayList<>(parser.getQueryArray());
        queryParse.setTokenQuery(queriesTokens);
        parser.getQueryArray().clear();
        ArrayList<String> descArray = new ArrayList<>();
        String desc = "";
        if (description) {
            desc = "<TEXT>" + queryParse.getDescription() + "</TEXT>";
            descArray.add(desc);
        }
        parser.parseDocs(descArray);
        queryParse.setTokenDesc(new ArrayList<>(parser.getQueryArray()));

        return queryParse;
    }

    /**
     * this function return is stemming option was selected
     * @return true or false is stem option was selected
     */
    public boolean isStem() {
        return stem;
    }

    /**
     * this function
     * @param docsRanks
     * @param queryList
     * @param semanticSelected
     * @throws IOException
     */
    public void relevantDocs(Map<String, Map<String, Double>> docsRanks, List<Query> queryList, boolean semanticSelected) throws IOException {
        Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
        com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (Query query : queryList) {
            QueryRun queryRun = new QueryRun(query, docsRanks, semanticSelected, stem, description, semanticSearcher,this.docLength);
            executor.execute(new Thread(queryRun));
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

    public Map<String, Double> topFifty(Map<String, Double> docRanked) {
        Map<String, Double> docRankCopy = new HashMap<>(docRanked);
        if (docRanked.size() > 50) {
            int numberOfdocs = 0;
            Map<String, Double> topFifty = new LinkedHashMap<>();
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

    public void singleQueryRank(Query singleQuery, Map<String, Map<String, Double>> docsRanks, boolean semanticSelected) throws IOException {
        Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
        com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
        QueryRun queryRun = new QueryRun(singleQuery, docsRanks, semanticSelected, stem, description, semanticSearcher,this.docLength);
        queryRun.run();
    }
}

