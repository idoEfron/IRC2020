package Model;

public class Token {
    private String str;
    private int length;
    private String docId;
    private boolean inTitle;
    private String file;
    private String date;

    /**
     * this function is the constructor of the token class
     * @param str the term of the token
     * @param docId the document id of the token
     * @param date the date of the token
     * @param inTitle if the token appears in the title
     * @param fileName the file name of the token
     */
    public Token(String str, String docId, String date, boolean inTitle, String fileName) {
        this.str = str;
        this.length = str.length();
        this.docId = docId;
        this.inTitle = inTitle;
        this.date = date;
        file = fileName;
    }

    /**
     * this function  checks if 2 tokens are equals
     * @param o the other  object that needed to be compered
     * @return true or false is the tokens are equals
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return str.equals(token.str);
    }

    /**
     * this function is a setter
     * @param str the string to set
     */
    public void setStr(String str) {
        this.str = str;
    }

    /**
     * this function is a setter
     * @param length the length of the token
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * this function is a setter
     * @param docId the document id that needed to be set
     */
    public void setDocId(String docId) {
        this.docId = docId;
    }

    /**
     * this function is a setter
     * @param inTitle if the term in the title
     */
    public void setInTitle(boolean inTitle) {
        this.inTitle = inTitle;
    }

    /**
     * this function return the hashcode of the token
     * @return the string that represent the hash code
     */
    @Override
    public int hashCode() {
        return this.str.hashCode();
    }

    /**
     * this function is the getter of the docid
     * @return the docid
     */
    public String getDocId() {
        return docId;
    }

    /**
     * this function is a getter
     * @return the string of the token
     */
    public String getStr() {
        return str;
    }

    /**
     * rthis function is a getter
     * @return the boolean if the token appease in the title
     */
    public boolean isInTitle() {
        return inTitle;
    }

    /**
     * this function is a getter that gets the length of the token
     * @return the length of the token
     */
    public int getLength() {
        return length;
    }

    /**
     * this function is a getter that get the file
     * @return the file name of the token
     */
    public String getFile() {
        return file;
    }
}
