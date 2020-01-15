package Model;

import java.util.ArrayList;
import java.util.List;

public class Query {
    private String numOfQuery;
    private String title;
    private String description;
    private  String narrative;
    private ArrayList<String> tokenQuery;
    private ArrayList<String> tokenDesc;

    /**
     * this function is the constructor if the class
     * @param numOfQuery the number of the query
     * @param title the title of the query
     * @param description the description of the query
     * @param narrative the narrative of the query
     */
    public Query(String numOfQuery, String title, String description, String narrative) {
        this.numOfQuery = numOfQuery;
        this.title = title;
        this.description = description;
        this.narrative = narrative;
        this.tokenQuery = new ArrayList<>();
        this.tokenDesc= new ArrayList<>();
    }

    /**
     * this function is a getter of the array of token query
     * @return the tokenQuery
     */
    public ArrayList<String> getTokenQuery() {
        return new ArrayList<>(tokenQuery);
    }

    /**
     * this function is a setter that set the query array
     * @param tokenQuery the tokenQuery that need to be set
     */
    public void setTokenQuery(ArrayList<String> tokenQuery) {
        for (String str:tokenQuery)
        {
         this.tokenQuery.add(str);
        }
    }

    /**
     * this function set the token description arrayList
     * @param tokenQuery
     */
    public void setTokenDesc(ArrayList<String> tokenQuery) {
        for (String str:tokenQuery)
        {
            this.tokenDesc.add(str);
        }
    }

    /**
     * this function is a getter that return the query number
     * @return the query number
     */
    public String getNumOfQuery() {
        return numOfQuery;
    }

    /**
     * this function is a getter that get the token description list
     * @return token description list
     */
    public ArrayList<String> getTokenDesc() {
        return tokenDesc;
    }

    /**
     * this function is a getter that return the title of the query
     * @return the title of the query
     */
    public String getTitle() {
        return title;
    }

    /**
     * this function return the Description of the query
     * @return the Description query
     */
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

