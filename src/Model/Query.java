package Model;

import java.util.ArrayList;
import java.util.List;

public class Query {
    private String numOfQuery;
    private String title;
    private String description;
    private  String narrative;
    private ArrayList<String> tokenQuery;

    public Query(String numOfQuery, String title, String description, String narrative) {
        this.numOfQuery = numOfQuery;
        this.title = title;
        this.description = description;
        this.narrative = narrative;
        this.tokenQuery = new ArrayList<>();
    }

    public ArrayList<String> getTokenQuery() {
        return tokenQuery;
    }

    public void setTokenQuery(ArrayList<String> tokenQuery) {
        for (String str:tokenQuery)
        {
         this.tokenQuery.add(str);
        }
    }

    public String getNumOfQuery() {
        return numOfQuery;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNumOfQuery(String numOfQuery) {
        this.numOfQuery = numOfQuery;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}

