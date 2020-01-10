package Model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Searcher {
    private Parser parser;
    private ReadFile readFile;
    private Indexer indexer;
    private String query;
    private ArrayList<String> queriesTokens;

    public Searcher(String query, String stopWordPath, Boolean stem) throws IOException, ParseException {
        indexer = new Indexer(stem, stopWordPath);
        readFile = new ReadFile(null, indexer, stem, stopWordPath);
        parser = new Parser(stem, readFile, stopWordPath, indexer, true);
        this.query = query;
        queriesTokens = new ArrayList<>();
    }

    public ArrayList<String> getQueriesTokens() {
        return queriesTokens;
    }

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
                            numOfQuery = line.substring(line.indexOf(":")+1);
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
        for (int i=0;i<listQueries.size();i++) {
            Query query = listQueries.get(i);
            queriesTokens=new ArrayList<>();
            String title = "<TEXT>"+query.getTitle()+"</TEXT>";
            ArrayList<String> temp = new ArrayList<>();
            temp.add(title);
            parser.parseDocs(temp);
            queriesTokens = parser.getQueryArray();
            query.setTokenQuery(parser.getQueryArray());
        }
        return listQueries;
    }
    public Query startSingleQuery() throws ParseException, InterruptedException, IOException {
        String q = query;
        Query queryParse=null;
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
                            numOfQuery = line.substring(line.indexOf(":")+1);
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
        String title = "<TEXT>"+queryParse.getTitle()+"</TEXT>";
        ArrayList<String> temp = new ArrayList<>();
        temp.add(title);
        parser.parseDocs(temp);
        queryParse.setTokenQuery(parser.getQueryArray());
        queriesTokens = parser.getQueryArray();
        return queryParse;
    }
}

