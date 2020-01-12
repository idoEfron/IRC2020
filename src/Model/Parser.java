package Model;

import snowball.ext.porterStemmer;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

public class Parser {

    private Map<Token, Map<String, ArrayList<String>>> termMap;
    private HashSet<String> stopwords;
    private static Map<String, Map<String, ArrayList<String>>> entities = new HashMap<>();
    private Map<String, String> months;
    private Map<String, String> mass;
    private Map<String, String> electrical;
    private boolean stemming;
    private Map<String, Integer> maxTf;
    private Map<String, Integer> wordCounter;
    private Set<String> termsInDoc;
    private ArrayList<String> queryArray;
    private ReadFile rf;
    private Indexer indexer;
    private boolean isQuery;
    private String fileName;
    private Map<String, Map<String, String>> entitiesPerDoc;
    private static  int blockNum = 0;
    //private Map<String, Set<String>> topFiveEntitiesDocs;
    private Map<String, Integer> docLength;
    private static Semaphore mutex = new Semaphore(1);

    public Map<String, Integer> getDocLength() {
        return docLength;
    }

    /**
     * this is the constructor of the parser
     *
     * @param stem          the boolean that return true or false
     * @param readFile      the readFile Object
     * @param stopWordsPath the stopWords
     * @param indexer       the indexer object
     * @throws IOException
     * @throws ParseException
     */
    public Parser(boolean stem, ReadFile readFile, String stopWordsPath, Indexer indexer, boolean isQuery) throws IOException, ParseException {
        this.indexer = indexer;
        rf = readFile;
        wordCounter = new HashMap<>();
        termsInDoc = new HashSet<>();
        queryArray = new ArrayList<>();
        stemming = stem;
        docLength = new HashMap<>();
        this.isQuery = isQuery;
        maxTf = new HashMap<>();
        termMap = new LinkedHashMap<>();
        stopwords = new HashSet<String>();
        entitiesPerDoc = new HashMap<>();
        if (rf.getSubFolder() == null) {
            fileName = "query";
        } else {
            fileName = rf.getSubFolder().get(0).getName();
        }
        months = new HashMap<String, String>() {{
            put("January", "01");
            put("JANUARY", "01");
            put("JAN", "01");
            put("Jan", "01");
            put("February", "02");
            put("FEBRUARY", "02");
            put("Feb", "02");
            put("FEB", "02");
            put("March", "03");
            put("MARCH", "03");
            put("MAR", "03");
            put("Mar", "03");
            put("April", "04");
            put("APRIL", "04");
            put("APR", "04");
            put("Apr", "04");
            put("May", "05");
            put("MAY", "05");
            put("June", "06");
            put("JUNE", "06");
            put("JUN", "06");
            put("Jun", "06");
            put("July", "07");
            put("JULY", "07");
            put("JUL", "07");
            put("Jul", "07");
            put("August", "08");
            put("AUGUST", "08");
            put("AUG", "08");
            put("Aug", "08");
            put("September", "09");
            put("SEPTEMBER", "09");
            put("Sep", "09");
            put("SEP", "09");
            put("October", "10");
            put("OCTOBER", "10");
            put("OCT", "10");
            put("Oct", "10");
            put("November", "11");
            put("NOVEMBER", "11");
            put("NOV", "11");
            put("Nov", "11");
            put("December", "12");
            put("DECEMBER", "12");
            put("DEC", "12");
            put("Dec", "12");
        }};
        mass = new HashMap<String, String>() {{
            put("kilogram", "KG");
            put("kilograms", "KG");
            put("kg", "KG");
            put("KG", "KG");
            put("Kilogram", "KG");
            put("Kilograms", "KG");
            put("grams", "G");
            put("gram", "G");
            put("Grams", "G");
            put("Gram", "G");
            put("milligram", "MG");
            put("Milligram", "MG");
            put("milligrams", "MG");
            put("Milligrams", "MG");
            put("milligram", "MG");
            put("Milligram", "MG");
            put("milligrams", "MG");
            put("Milligrams", "MG");
            put("ton", "T");
            put("Tons", "T");
            put("Ton", "T");
            put("tons", "T");

        }};

        electrical = new HashMap<String, String>() {{
            put("milliampere", "mA");
            put("Milliampere", "mA");
            put("milli-ampere", "mA");
            put("Milli-ampere", "mA");
            put("milliampere".toUpperCase(), "mA");
            put("Milli-ampere".toUpperCase(), "mA");
            put("Watt", "W");
            put("watt", "W");
            put("watt".toUpperCase(), "W");
            put("volt", "V");
            put("Volt", "V");
            put("VOlT", "V");
            put("Kilogram".toUpperCase(), "KG");
            put("megawatt", "MW");
            put("Megawatt", "MW");
            put("Megawatt".toUpperCase(), "MW");
            put("Mega-watt", "MW");
            put("mega-watt", "MW");
            put("Mega-watt".toUpperCase(), "MW");
            put("kilowatt", "KW");
            put("Kilowatt", "KW");
            put("kilowatt".toUpperCase(), "KW");
            put("Kilo-watt", "KW");
            put("kilo-watt", "KW");
            put("kilo-watt".toUpperCase(), "KW");

        }};

        ///add stopwords to hashset
        /*
        this part of the code is from https://howtodoinjava.com/java/io/read-file-from-resources-folder/
         */
        //String fileName = "stopwords.txt";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (this.stopwords.size() == 0) {
            File stopWordsFile = new File(stopWordsPath + "/stop_words.txt");/****************////////////////
            String stopContent = new String(Files.readAllBytes(stopWordsFile.toPath()));
            String stopLines[] = stopContent.split("\\r?\\n");
            stopwords.addAll(Arrays.asList(stopLines));
        }
        //System.out.println(stopwords.size());
        ////call the parseDocs function
    }

    /**
     * this function is responsibly is to split the documents to tokens
     *
     * @param docList that needed to be parse and splited
     */
    public void parseDocs(ArrayList<String> docList) throws ParseException, IOException, InterruptedException {
        if (isQuery) {
            termMap.clear();
        }
        String docNo = "";
        String title = "";
        String date = "";
        for (int i = 0; i < docList.size(); i++) {
            if (!docList.get(i).equals("\n") && !docList.get(i).equals("\n\n\n") && !docList.get(i).equals("\n\n\n\n") && !docList.get(i).equals("\n\n")) {
                String docId = docList.get(i);
                docNo = "";
                title = "";
                date = "";

                if (docId.contains("<DOCNO>")) {
                    docNo = docId.substring(docId.indexOf("<DOCNO>") + 8, docId.indexOf("</DOCNO>") - 1);
                }
                if (docId.contains("<TI>")) {
                    title = docId.substring(docId.indexOf("<TI>"), docId.indexOf("</TI>"));
                    title = title.replaceAll("\\<.*?\\>", "");
                }
                if (docId.contains("<DATE1>")) {
                    date = docId.substring(docId.indexOf("<DATE1>"), docId.indexOf("</DATE1>"));
                    date = date.replaceAll("\\<.*?\\>", "");
                    String[] dateSplit = date.split(" ");
                    if (dateSplit.length == 3 && months.containsKey(dateSplit[1])) {
                        dateSplit[1] = months.get(dateSplit[1]);
                        date = dateSplit[0] + "/" + dateSplit[1] + "/" + dateSplit[2];
                    }

                }
                if (docId.contains("<DATE>")) {
                    String s = docId.substring(docId.indexOf("<DATE>"), docId.indexOf("\n</DATE>"));
                    // s = s+ "</DATE>";
                    s = s.replaceAll("\\<.*?\\>", "");
                    if (s.contains(",")) {
                        String[] dates = s.split(",");
                        s = dates[0].substring(2) + " " + dates[1];
                    }
                    date = s;
                }
                String txt = "";
                if (docId.contains("<TEXT>") && docId.contains("</TEXT>")) {
                    txt = docId.substring(docId.indexOf("<TEXT>"), docId.indexOf("</TEXT>"));
                    txt = txt.replaceAll("\\<.*?\\>|\\p{Ps}|\\p{Pe}", " ");
                    txt = txt.replaceAll(":", " ");
                    txt = txt.replace("--", " ");
                    String[] tokens = txt.split("\\s+|\n");
                    docLength.put(docNo, tokens.length);
                    ArrayList<Token> afterCleaning = new ArrayList<>();
                    for (int y = 0; y < tokens.length; y++) {
                        String currToken = tokens[y];
                        String token = "";
                        boolean inTitle = false;
                        if (title.contains(currToken)) {
                            inTitle = true;
                        }
                        if (currToken.contains("/") && (Character.isDigit(currToken.charAt(0)) == false || Character.isDigit(currToken.charAt(currToken.length() - 1)) == false)) {
                            String[] afterRemoving = currToken.split("/");
                            for (int j = 0; j < afterRemoving.length; j++) {
                                token = cleanToken(afterRemoving[j]);
                                if (token.length() > 0) {
                                    afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                }//token,token.length(),docList.get(i).indexOf(token)
                            }

                        } else if (!currToken.contains("U.S") && !isNumber(currToken) && !(currToken.contains("%") || currToken.contains("$"))
                                && !currToken.matches("[a-zA-Z0-9]a-zA-Z0-9]*")) {
                            if (isNumric(currToken) == false && isBothNumber(currToken) == false) {
                                if (checkBetween(currToken) == false) {
                                    String[] afterRemoving = currToken.split("\\W");
                                    if (afterRemoving.length > 1) {
                                        if (afterRemoving.length == 2 && (isNumric(afterRemoving[0]) && isNumric(afterRemoving[1])) ||
                                                (isNumric(afterRemoving[1]) && afterRemoving[0].equals("") && afterRemoving[1].length() + 1 == currToken.length()) ||
                                                (isNumric(afterRemoving[0]) && afterRemoving[1].contains("m"))) {
                                            afterCleaning.add(new Token(currToken, docNo, date, title.contains(currToken), fileName));
                                        } else {
                                            for (int j = 0; j < afterRemoving.length; j++) {
                                                token = cleanToken(afterRemoving[j]);
                                                if (token.length() > 0) {
                                                    afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                                }
                                            }
                                        }
                                    } else if (afterRemoving.length == 1) {
                                        token = cleanToken(afterRemoving[0]);
                                        afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                    }
                                } else {
                                    token = cleanToken(currToken);
                                    if (token.contains("-") && checkBetween(token)) {
                                        String[] arrToken = token.split("-");
                                        if (arrToken.length == 2) {
                                            afterCleaning.add(new Token(arrToken[0], docNo, date, title.contains(arrToken[0]), fileName));
                                            afterCleaning.add(new Token(arrToken[1], docNo, date, title.contains(arrToken[1]), fileName));
                                            if (y - 1 >= 0 && isNumber(tokens[y - 1]) && (arrToken[0].equalsIgnoreCase("Thousand") || arrToken[0].equalsIgnoreCase("Million") || arrToken[0].equalsIgnoreCase("Billion"))) {
                                                token = addMeasure(tokens[y - 1], token);
                                            }
                                            if (y + 1 < tokens.length && isNumber(arrToken[1]) && (tokens[y + 1].equalsIgnoreCase("Thousand") || tokens[y + 1].equalsIgnoreCase("Million") || tokens[y + 1].equalsIgnoreCase("Billion"))) {
                                                token = addMeasure(tokens[y + 1], token);
                                            }
                                            afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));

                                        }
                                        if (arrToken.length == 3 && Character.isLetter(arrToken[0].charAt(0)) && Character.isLetter(arrToken[1].charAt(0)) && Character.isLetter(arrToken[2].charAt(0))) {
                                            afterCleaning.add(new Token(arrToken[0], docNo, date, title.contains(arrToken[0]), fileName));
                                            afterCleaning.add(new Token(arrToken[1], docNo, date, title.contains(arrToken[1]), fileName));
                                            afterCleaning.add(new Token(arrToken[2], docNo, date, title.contains(arrToken[2]), fileName));
                                            afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                        }
                                    }
                                }
                            } else if (isNumric(currToken)) {
                                afterCleaning.add(new Token(currToken, docNo, date, title.contains(currToken), fileName));
                            } else {
                                token = cleanToken(currToken);
                                if (token.contains("-")) {
                                    if (isBothNumber(token)) {
                                        String[] arrToken = token.split("-");
                                        afterCleaning.add(new Token(arrToken[0], docNo, date, title.contains(arrToken[0]), fileName));
                                        afterCleaning.add(new Token(arrToken[1], docNo, date, title.contains(arrToken[1]), fileName));
                                        afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                    }
                                } else {
                                    if (token.length() > 0) {
                                        afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                                    }
                                }
                            }
                        } else {
                            token = cleanToken(tokens[y]);
                            if (token.length() > 0) {
                                afterCleaning.add(new Token(token, docNo, date, title.contains(token), fileName));
                            }
                        }//bracket on the else
                    }//for on the tokens after split
                    entitiesPerDoc.put(docNo, new HashMap<>());//todo ido add
                    handler(afterCleaning, docNo, date, title);
                }
            }
            wordCounter.put(docNo, termsInDoc.size());
            termsInDoc.clear();
        }//bracket on the for on the doc list's
        mutex.acquire();
        blockNum++;
        mutex.release();
        if (isQuery==false) {
            File mergeDocLoction = new File(indexer.getPostingPath() + "/docsEnts");
            if(!mergeDocLoction.exists()){
                mergeDocLoction.mkdir();
            }
            String postingPath = indexer.getPostingPath() + "/docsEnts" + "/" + blockNum +".txt";
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(postingPath, true)));
            for (String docsId : entitiesPerDoc.keySet()) {
                String ent = entitiesPerDoc.get(docsId).toString();
                writer.append(docsId + " : " + ent + "\n");
            }
            writer.flush();
            writer.close();
        }
        months.clear();
        mass.clear();
        electrical.clear();
        if (isQuery) {
            queryArray = new ArrayList<>();
            for (Token t : termMap.keySet()) {
                if (!stopwords.contains(t.getStr().toLowerCase())) {
                    queryArray.add(t.getStr());
                }
            }
        } else {
            indexer.addBlock(this);
        }
    }

    public ArrayList<String> getQueryArray() {
        return queryArray;
    }

    private String addMeasure(String addTo, String tokenToAdd) throws ParseException {
        if (tokenToAdd.contains("-")) {
            String[] arrTokens = tokenToAdd.split("-");
            if (isNumber(addTo)) {
                if (arrTokens[0].contains("Thousand") || arrTokens[0].contains("Thousand".toLowerCase()) || arrTokens[0].contains("Thousand".toUpperCase())) {
                    tokenToAdd = addTo + "K" + "-" + arrTokens[1];
                } else if ((arrTokens[0].contains("Million") || arrTokens[0].contains("Million".toLowerCase()) || arrTokens[0].contains("Million".toUpperCase()))) {
                    tokenToAdd = addTo + "M" + "-" + arrTokens[1];
                } else if ((arrTokens[0].contains("Billion") || arrTokens[0].contains("Billion".toLowerCase()) || arrTokens[0].contains("Billion".toUpperCase()))) {
                    tokenToAdd = addTo + "B" + "-" + arrTokens[1];
                }
                return tokenToAdd;
            } else if (isNumber(arrTokens[1]) && (addTo.equalsIgnoreCase("Thousand") || addTo.equalsIgnoreCase("Million") || addTo.equalsIgnoreCase("Billion"))) {
                if (addTo.contains("Thousand") || addTo.contains("Thousand".toLowerCase()) || addTo.contains("Thousand".toUpperCase())) {
                    tokenToAdd = arrTokens[0] + "-" + arrTokens[1] + "K";
                } else if ((addTo.contains("Million") || addTo.contains("Million".toLowerCase()) || addTo.contains("Million".toUpperCase()))) {
                    tokenToAdd = arrTokens[0] + "-" + arrTokens[1] + "M";
                } else if ((addTo.contains("Billion") || addTo.contains("Billion".toLowerCase()) || addTo.contains("Billion".toUpperCase()))) {
                    tokenToAdd = arrTokens[0] + "-" + arrTokens[1] + "B";
                }
                return tokenToAdd;
            }
        } else if (isNumber(addTo) && tokenToAdd.equalsIgnoreCase("Thousand") || tokenToAdd.equalsIgnoreCase("Million") || tokenToAdd.equalsIgnoreCase("Billion")) {
            if (tokenToAdd.contains("Thousand") || tokenToAdd.contains("Thousand".toLowerCase()) || tokenToAdd.contains("Thousand".toUpperCase())) {
                tokenToAdd = addTo + "K";
            } else if ((tokenToAdd.contains("Million") || tokenToAdd.contains("Million".toLowerCase()) || tokenToAdd.contains("Million".toUpperCase()))) {
                tokenToAdd = addTo + "M";
            } else if ((tokenToAdd.contains("Billion") || tokenToAdd.contains("Billion".toLowerCase()) || tokenToAdd.contains("Billion".toUpperCase()))) {
                tokenToAdd = addTo + "B";
            }
        }
        return tokenToAdd;
    }

    private boolean checkBetween(String currToken) throws ParseException {
        if (currToken.contains("-")) {
            String[] arrTokens = currToken.split("-");
            if (arrTokens.length == 2) {
                if ((isNumber(arrTokens[0]) || isNumber(arrTokens[1]))) {
                    return true;
                }
                if (!isNumber(arrTokens[0]) && !isNumber(arrTokens[1])) {
                    return true;
                }
                return false;
            } else if (arrTokens.length == 3 && !isNumber(arrTokens[0]) && !isNumber(arrTokens[1]) && !isNumber(arrTokens[2])) {
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * this function chack if the number is numric
     *
     * @param currToken the token that needed to be checked
     * @return true or false
     */
    private boolean isNumric(String currToken) throws ParseException {
        try {
            Double.parseDouble(currToken);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBothNumber(String currToken) throws ParseException {
        if (currToken.contains("-")) {
            String[] arrTokens = currToken.split("-");
            if (arrTokens.length == 2) {
                if (isNumber(arrTokens[0]) && isNumber(arrTokens[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * this function handel all of the tokens and parse each one
     *
     * @param terms all of the term in a document
     * @param docID the document id
     * @param date  the date of the document
     * @param title the title of the document
     * @throws ParseException
     * @throws InterruptedException
     */
    private void handler(ArrayList<Token> terms, String docID, String date, String title) throws ParseException, InterruptedException {
        for (int i = 0; i < terms.size(); i++) {
            if (terms.get(i).getStr().length() > 0) {
                if (!(numberHandler(terms, i, docID, date, title))) {
                    stringHandler(terms, i, docID, date, title);

                }
                boolean inTitle = false;
                if (terms.get(i).getStr().contains("-")) {
                    //termMap.put(terms.get(i), new HashMap<String, Integer>());
                    //termMap.get(terms.get(i)).put(docID, 1);
                    String[] strArray = terms.get(i).getStr().split("-");
                    ArrayList<Token> rangeList = new ArrayList<Token>();
                    for (int k = 0; k < strArray.length; k++) {
                        if (title.contains(strArray[k])) {
                            inTitle = true;
                        }
                        rangeList.add(new Token(strArray[k], docID, date, inTitle, fileName));
                    }
                    /*for (int j = 0; j < rangeList.size(); j++) {
                        if (rangeList.get(j).getStr().equals("")) {
                            rangeList.remove(j);
                        }
                    }*/
                    handler(rangeList, docID, date, title);
                }
            }
        }
    }

    /**
     * this function is cleaning the token
     *
     * @param token that needed to be clean
     * @return
     */
    protected String cleanToken(String token) {
        if (token.length() > 0 && checkChar(token.charAt(0)) == false) {
            token = token.substring(1, token.length());
        }
        if (token.length() > 0 && checkChar(token.charAt(token.length() - 1)) == false) {
            token = token.substring(0, token.length() - 1);
        }
        if (token.length() > 0 && (checkChar(token.charAt(0)) == false || checkChar(token.charAt(token.length() - 1)) == false)) {
            token = cleanToken(token);
        }
        if (token.length() == 1 && (token.equals("%") || token.equals("$"))) {
            token = "";
        }
        return token;
    }


    /**
     * this function check if the char is a comma
     *
     * @param charAt
     * @return
     */
    private boolean checkChar(char charAt) {
        return ((charAt >= 65 && charAt <= 90) || (charAt >= 97 && charAt <= 122) ||
                (charAt >= 48 && charAt <= 57) || charAt == '$' || charAt == '%');

    }

    // *************change public to private***********

    /**
     * checks if the input number is indeed a number
     *
     * @param str
     * @return
     * @throws ParseException
     */
    public boolean isNumber(String str) throws ParseException {
        if (str.length() > 0 && Character.isDigit(str.charAt(0))) {
            Pattern pattern = Pattern.compile("\\d+(,\\d+)*(\\.\\d+)?");
            if (str.matches("\\d+(,\\d+)*(\\.\\d+)?")) {

                return true;
            }
        }

        return false;
    }

    /**
     * this function assist the hendler function and hendle all of the token that represent numbers
     *
     * @param tokens the token that being handle
     * @param index  the index of the token
     * @param docID  the document id fot the token
     * @param date   the date of the document id
     * @param title  the title of the document
     * @return if the number handker succeeded
     * @throws ParseException
     */
    public boolean numberHandler(ArrayList<Token> tokens, int index, String docID, String date, String title) throws ParseException {
        String before = "";
        String current = tokens.get(index).getStr();
        String after = "";
        String afterTwo = "";
        String afterThree = "";
        String num = current.replaceAll(",", "");

        if (index > 0) {
            before = tokens.get(index - 1).getStr();
        }
        if (index < tokens.size() - 1) {
            after = tokens.get(index + 1).getStr();
        }
        if (index < tokens.size() - 2) {
            afterTwo = tokens.get(index + 2).getStr();
        }
        if (index < tokens.size() - 3) {
            afterThree = tokens.get(index + 3).getStr();
        }

        // checks literal number cases

        if (isNumber(current) || current.contains("$") || current.contains("/") || (Character.isDigit(current.charAt(0)) && (Character.compare(current.charAt(current.length() - 1), 'm') == 0)) ||
                (current.contains("bn") && after.equals("Dollars")) || current.contains("%")) {
            if (after.contains("Thousand") || after.contains("Thousand".toLowerCase()) || after.contains("Thousand".toUpperCase())) {
                putTerm(current, "K", docID, date, title);
            } else if (!current.contains("$") && !afterTwo.equals("U.S.") && !afterThree.equals("dollars") && (after.contains("Million") || after.contains("Million".toLowerCase()) || after.contains("Million".toUpperCase()))) {
                putTerm(current, "M", docID, date, title);
            } else if (!afterThree.equals("dollars") && !afterTwo.equals("U.S.") && !current.contains("$") && (after.contains("Billion") || after.contains("Billion".toLowerCase()) || after.contains("Billion".toUpperCase()))) {
                putTerm(current, "B", docID, date, title);
            }
            //***************checks if the case is percentage***************************////
            else if (after.contains("percent") || after.contains("percentage") ||
                    after.contains("Percentage") || after.contains("Percent")) {
                putTerm(current, "%", docID, date, title);
                return true;
            } else if (current.contains("%")) {
                if (Character.compare(current.charAt(current.length() - 1), '%') == 0 && isNumber(current.substring(0, current.indexOf('%')))) {
                    putTerm(current, "", docID, date, title);
                }
            }
            /***************precent********************************/////
            //checks if expression is mass units
            else if (mass.containsKey(after)) {
                putTerm(current, mass.get(after), docID, date, title);
            }
            //checks if expression is electrical units
            else if (electrical.containsKey(after)) {
                putTerm(current, electrical.get(after), docID, date, title);
            }
            try {


                //checks if expression is date
                if (Integer.parseInt(current) <= 31 && Integer.parseInt(current) >= 0 && months.containsKey(after)) {
                    putTerm(months.get(after) + "-", current, docID, date, title);
                }
            } catch (NumberFormatException e) {

            }
            //*******************dollars************************************************
            try {
                if (after.equals("Dollars") ||
                        current.contains("$") || (after.equals("billion") && afterTwo.equals("U.S.")
                        && afterThree.equals("dollars")) || (after.equals("million") && afterTwo.equals("U.S."))
                        && afterThree.equals("dollars")) {

                    if (current.contains("$")) {
                        current = current.substring(1);
                        String numDub = current.replaceAll(",", "");
                        if ((Double.parseDouble(numDub) < 1000000)) {
                            if (!after.equals("million") && !after.equals("billion")) {
                                putTerm(current, " Dollars", docID, date, title);
                                return true;
                            } else if (after.equals("million")) {
                                putTerm(current, " M Dollars", docID, date, title);
                                return true;
                            } else if (after.equals("billion")) {
                                putTerm(current + "000", " M Dollars", docID, date, title);
                                return true;
                            }
                        } else if (Double.parseDouble(numDub) >= 1000000) {
                            current = format(current);
                            putTerm(current, " M Dollars", docID, date, title);
                            return true;
                        }
                    }////$$$$
                    else if (after.equals("Dollars")) {
                        if (current.contains("m")) {
                            current = current.substring(0, current.length() - 1);
                            putTerm(current, " M Dollars", docID, date, title);
                            return true;
                        } else if (current.contains("bn")) {
                            current = current.substring(0, current.length() - 2);
                            putTerm(current + "000", " M Dollars", docID, date, title);
                            return true;
                        }
                        String numDub = current.replaceAll(",", "");
                        if (!current.contains("/") && Double.parseDouble(numDub) >= 1000000) {
                            current = format(current);
                            putTerm(current, " M Dollars", docID, date, title);
                            return true;
                        } else if (!current.contains("/") && Double.parseDouble(numDub) < 1000000) {
                            putTerm(current, " Dollars", docID, date, title);
                            return true;
                        } else if (isNumber(before) && current.contains("/")) {
                            putTerm(before + " " + current, " Dollars", docID, date, title);
                            return true;
                        }
                    }
                    ////////*******dollars********///////////////////
                    else if (isNumber(current) && after.equals("billion") && afterTwo.equals("U.S.") && afterThree.equals("dollars")) {
                        putTerm(current + "000", " M Dollars", docID, date, title);
                        return true;
                    } else if (isNumber(current) && after.equals("million") && afterTwo.equals("U.S.") && afterThree.equals("dollars")) {
                        putTerm(current, " M Dollars", docID, date, title);
                        return true;
                    }
                }
                ///********************************************************************************
                //regular number
                if (current.contains("/")) {
                    putTerm(current, "", docID, date, title);
                } else if (Double.parseDouble(num) >= 1000) {
                    int counter = 0;
                    for (int i = 0; i < current.length(); i++) {
                        char theChar = current.charAt(i);
                        if (Character.compare(theChar, ',') == 0) {
                            counter++;
                        }
                    }

                    // handle case of number without additional word (such as thousand, million and etc..)
                    if (counter > 0) {

                        current = format(current);///*****////

                        switch (counter) {
                            case 1:
                                current = current + "K";
                                break;
                            case 2:
                                current = current + "M";
                                break;
                            case 3:
                                current = current + "B";
                                break;
                            default:
                                break;
                        }
                        Boolean inTitle = title.contains(current);
                        Token currToken = new Token(current, docID, date, inTitle, fileName);
                        if (termMap.containsKey(currToken)) {
                            if (termMap.get(currToken).containsKey(docID)) {
                                termMap.get(currToken).get(docID).set(0, String.valueOf(Integer.parseInt(termMap.get(currToken).get(docID).get(0)) + 1));
                                return true;

                            } else {
                                termMap.get(currToken).put(docID, new ArrayList<>(3));
                                termMap.get(currToken).get(docID).add(0, "1");
                                termMap.get(currToken).get(docID).add(1, String.valueOf(Boolean.compare(inTitle, false)));
                                termMap.get(currToken).get(docID).add(2, date);
                                return true;

                            }
                        } else {
                            termMap.put(currToken, new HashMap<String, ArrayList<String>>());
                            termMap.get(currToken).put(docID, new ArrayList<>(3));
                            termMap.get(currToken).get(docID).add(0, "1");
                            termMap.get(currToken).get(docID).add(1, String.valueOf(Boolean.compare(inTitle, false)));
                            termMap.get(currToken).get(docID).add(2, date);
                            return true;
                        }
                    }
                } else if (Double.parseDouble(current) < 1000) {
                    if (!after.contains("/")) {
                        putTerm(current, "", docID, date, title);
                        return true;
                    } else if (after.contains("/") && !afterTwo.equals("Dollars")) {
                        putTerm(current, " " + after, docID, date, title);
                        tokens.remove(index + 1);
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                //wasn't able to parse term to double
            }
        } else if (current.contains("-")) {
            if (isBothNumber(current)) {
                String[] arrToken = current.split("-");
                String sumOne = arrToken[0].replaceAll(",", "");
                String sumTwo = arrToken[1].replaceAll(",", "");
                current = convertToNum(sumOne, arrToken[0]) + "-" + convertToNum(sumTwo, arrToken[1]);
                putTerm(current, "", docID, date, title);
            }
        }
        return false;
    }

    private String convertToNum(String num, String current) throws ParseException {


        if (Double.parseDouble(num) >= 1000) {
            int counter = 0;
            for (int i = 0; i < current.length(); i++) {
                char theChar = current.charAt(i);
                if (Character.compare(theChar, ',') == 0) {
                    counter++;
                }
            }
            // handle case of number without additional word (such as thousand, million and etc..)
            if (counter > 0) {
                current = format(current);///*****////

                switch (counter) {
                    case 1:
                        current = current + "K";
                        break;
                    case 2:
                        current = current + "M";
                        break;
                    case 3:
                        current = current + "B";
                        break;
                    default:
                        break;
                }
            }
        }
        return current;
    }

    /**
     * assisting function to number handler to put term in the term dictionary
     *
     * @param current   the current tokrn that needed to be putted
     * @param character the string that nedded to be concatenate
     * @param docId     the document id of the term
     * @param date      the date of the document
     * @param title     the title of the document
     */
    private void putTerm(String current, String character, String docId, String date, String title) {
        boolean inTitle = title.contains(current + character);
        Token currToken = new Token(current + character, docId, date, inTitle, fileName);
        if (termMap.containsKey(currToken)) {
            if (termMap.get(currToken).containsKey(docId)) {
                termMap.get(currToken).get(docId).set(0, String.valueOf(Integer.parseInt(termMap.get(currToken).get(docId).get(0)) + 1));
                updateMaxTf(current, character, docId, date, title);
                updateWordList(current, character);

            } else {
                termMap.get(currToken).put(docId, new ArrayList<>(3));
                termMap.get(currToken).get(docId).add(0, "1");
                termMap.get(currToken).get(docId).add(1, String.valueOf(Boolean.compare(inTitle, false)));
                termMap.get(currToken).get(docId).add(2, date);
                updateMaxTf(current, character, docId, date, title);
                updateWordList(current, character);
            }

        } else {
            termMap.put(currToken, new HashMap<String, ArrayList<String>>());
            termMap.get(currToken).put(docId, new ArrayList<>(3));
            termMap.get(currToken).get(docId).add(0, "1");
            termMap.get(currToken).get(docId).add(1, String.valueOf(Boolean.compare(inTitle, false)));
            termMap.get(currToken).get(docId).add(2, date);
            updateMaxTf(current, character, docId, date, title);
            updateWordList(current, character);
        }
    }

    /**
     * this function update the tf of a term with concatenate character
     *
     * @param current   the term
     * @param character
     * @param docID     the document id of the term
     * @param date      the date of a document
     * @param title     the title of the document
     */
    public void updateMaxTf(String current, String character, String docID, String date, String title) {
        if (maxTf.containsKey(docID)) {
            maxTf.put(docID, Math.max(Integer.parseInt(termMap.get(new Token(current + character, docID, date, title.contains(current + character), fileName)).get(docID).get(0)), maxTf.get(docID)));

        } else {
            maxTf.put(docID, Integer.parseInt(termMap.get(new Token(current + character, docID, date, title.contains(current + character), fileName)).get(docID).get(0)));
        }
    }

    /**
     * this function update the term dictionary according to a term
     *
     * @param current   the term that needed to be placed
     * @param character the string that needed to be concatenated
     */
    public void updateWordList(String current, String character) {
        if (!termsInDoc.contains(current + character)) {
            termsInDoc.add(current + character);
        }

    }

    /**
     * this function check if a term is a entity
     *
     * @param tokens   th e token that needed to be treated
     * @param index    the index of the token
     * @param docID    the document id of the term
     * @param date     the date of the document
     * @param title    the title of the document
     * @param fileName the file name of the token
     * @throws InterruptedException
     */
    private void checkEntity(ArrayList<Token> tokens, int index, String docID, String date, String title, String fileName) throws InterruptedException {
        String entity = "";

        if (!stopwords.contains(tokens.get(index))) {
            while (tokens.size() > 0 && index < tokens.size() && tokens.get(index).getLength() > 0 && Character.isUpperCase(tokens.get(index).getStr().charAt(0))) {
                if ((index + 1) < tokens.size() && tokens.get(index + 1).getLength() > 0 && Character.isUpperCase(tokens.get(index + 1).getStr().charAt(0))) {
                    entity = entity + tokens.get(index).getStr() + " ";
                } else {
                    entity = entity + tokens.get(index).getStr();
                }

                index++;
            }

            if (tokens.size() > 0 && entity.split(" ").length < 5) {
                putTermEntity(entity, docID, date, title);//todo ido add
            }
        }
    }

    //todo ido add!!!!!!!!!!!!!!!!!!1
    private void putTermEntity(String entity, String docID, String date, String title) throws InterruptedException {
        mutex.acquire();
        if (entities.containsKey(entity.toUpperCase())) {
            if (entities.get(entity.toUpperCase()).containsKey(docID)) {
                entities.get(entity.toUpperCase()).get(docID).set(0, String.valueOf(Integer.parseInt(entities.get(entity.toUpperCase()).get(docID).get(0)) + 1));
                entitiesPerDoc.get(docID).put(entity.toUpperCase(), String.valueOf(Integer.parseInt(entities.get(entity.toUpperCase()).get(docID).get(0)) + 1));

            } else {
                entities.get(entity.toUpperCase()).put(docID, new ArrayList<String>(3));
                entities.get(entity.toUpperCase()).get(docID).add(0, "1");
                entities.get(entity.toUpperCase()).get(docID).add(1, String.valueOf(Boolean.compare(title.contains(entity), false)));
                entities.get(entity.toUpperCase()).get(docID).add(2, date);
                entitiesPerDoc.get(docID).put(entity.toUpperCase(), "1");
            }
            if (this.indexer.getTermDictionary().containsKey(entity.toUpperCase())) {
                Map<String, ArrayList<String>> tempMap = entities.remove(entity.toUpperCase());
                termMap.put(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName), tempMap);

            } else if (entities.get(entity.toUpperCase()).size() >= 2) {
                termMap.put(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName), entities.remove(entity.toUpperCase()));
            }
        } else if (termMap.containsKey(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)) && entity.split("[-:, ]").length > 1) {
            if (termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).containsKey(docID)) {
                termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).set(0, String.valueOf(Integer.parseInt(termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).get(0)) + 1));
                int num = Integer.parseInt(termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).get(0));
                entitiesPerDoc.get(docID).put(entity.toUpperCase(), String.valueOf(num));


            } else {
                termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).put(docID, new ArrayList<String>(3));
                termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).add(0, "1");
                termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).add(1, String.valueOf(Boolean.compare(title.contains(entity), false)));
                termMap.get(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName)).get(docID).add(2, date);
                entitiesPerDoc.get(docID).put(entity.toUpperCase(), "1");
            }
        } else {
            if (entity.split("[-:, ]").length > 1) {
                entities.put(entity.toUpperCase(), new HashMap<String, ArrayList<String>>());
                entities.get(entity.toUpperCase()).put(docID, new ArrayList<String>(3));
                entities.get(entity.toUpperCase()).get(docID).add(0, "1");
                entities.get(entity.toUpperCase()).get(docID).add(1, String.valueOf(Boolean.compare(title.contains(entity), false)));
                entities.get(entity.toUpperCase()).get(docID).add(2, date);
                entitiesPerDoc.get(docID).put(entity.toUpperCase(), "1");
            }
        }
        mutex.release();
    }

    /**
     * this function assist the hendler function and hendle all of the token that represent a string term
     *
     * @param tokens the token that needed to be handle
     * @param index  the index of the token
     * @param docID  the document id of the term
     * @param date   the date of the document
     * @param title  the title of the document
     * @return true of false if the the token was handled
     * @throws ParseException
     * @throws InterruptedException
     */
    public boolean stringHandler(ArrayList<Token> tokens, int index, String docID, String date, String title) throws ParseException, InterruptedException {

        String before = "";
        String current = tokens.get(index).getStr();
        String after = "";

        if (index > 0) {
            before = tokens.get(index - 1).getStr();
        }
        if (index < tokens.size() - 1) {
            after = tokens.get(index + 1).getStr();
        }

        if (months.containsKey(current) || !stopwords.contains(current.toLowerCase())) {
            int num = -1;
            try {
                num = Integer.parseInt(after);
                if (months.containsKey(current)) {
                    if (num > 0 && num <= 31) {
                        Token currTok = new Token(months.get(current) + "-" + after, docID, date, title.contains(months.get(current) + "-" + after), fileName);
                        if (termMap.containsKey(currTok)) {
                            if (termMap.get(currTok).containsKey(docID)) {
                                termMap.get(currTok).get(docID).set(0, String.valueOf(Integer.parseInt(termMap.get(currTok).get(docID).get(0)) + 1));
                                return true;
                            } else {
                                termMap.get(currTok).put(docID, new ArrayList<>(3));
                                termMap.get(currTok).get(docID).add(0, "1");
                                termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
                                termMap.get(currTok).get(docID).add(2, date);
                                return true;
                            }

                        } else {
                            termMap.put(currTok, new HashMap<String, ArrayList<String>>());
                            termMap.get(currTok).put(docID, new ArrayList<>(3));
                            termMap.get(currTok).get(docID).add(0, "1");
                            termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
                            termMap.get(currTok).get(docID).add(2, date);
                            return true;
                        }

                    } else if (num > 1900 && isValidDate(after)) {
                        Token currTok = new Token(after + "-" + months.get(current), docID, date, title.contains(after + "-" + months.get(current)), fileName);
                        if (months.containsKey(currTok)) {
                            if (termMap.get(currTok).containsKey(docID)) {
                                termMap.get(currTok).get(docID).set(0, String.valueOf(Integer.parseInt(termMap.get(currTok).get(docID).get(0)) + 1));
                                return true;
                            } else {
                                termMap.get(currTok).put(docID, new ArrayList<>(3));
                                termMap.get(currTok).get(docID).add(0, "1");
                                termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
                                termMap.get(currTok).get(docID).add(2, date);
                                return true;
                            }
                        } else {
                            termMap.put(currTok, new HashMap<String, ArrayList<String>>());
                            termMap.get(currTok).put(docID, new ArrayList<>(3));
                            termMap.get(currTok).get(docID).add(0, "1");
                            termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
                            termMap.get(currTok).get(docID).add(2, date);
                            return true;
                        }
                    }

                }

            } catch (NumberFormatException e) {
                //term is not a date
            }
            if (current.contains("-") && checkBetween(current)) {
                String[] arrToken = current.split("-");
                if (arrToken.length == 2 && (isNumber(arrToken[0]) || isNumber(arrToken[1]))) {
                    if (isNumber(arrToken[0])) {
                        String sumOne = arrToken[0].replaceAll(",", "");
                        current = convertToNum(sumOne, arrToken[0]) + "-" + arrToken[1];
                        putTerm(current, "", docID, date, title);
                        return true;
                    } else {
                        String sumTwo = arrToken[1].replaceAll(",", "");
                        current = arrToken[0] + "-" + convertToNum(sumTwo, arrToken[1]);
                        putTerm(current, "", docID, date, title);
                        return true;
                    }
                } else {
                    putTerm(current, "", docID, date, title);
                    return true;
                }
            }

            /***lower/upper**////
            if (Character.isUpperCase(current.charAt(0))) {
                checkEntity(tokens, index, docID, date, title, fileName);
                Token currTok = new Token(current.toLowerCase(), docID, date, title.contains(current.toLowerCase()), fileName);
                if (termMap.containsKey(currTok.getStr().toLowerCase())) {
                    putTermString(current.toLowerCase(), docID, stemming, date, title);
                } else if (termMap.containsKey(new Token(current.toUpperCase(), docID, date, title.contains(current.toUpperCase()), fileName))) {
                    putTermString(current.toUpperCase(), docID, stemming, date, title);
                } else {
                    putTermString(current.toUpperCase(), docID, stemming, date, title);
                    return true;
                }
            } else if (Character.isLowerCase(current.charAt(0))) {
                if (termMap.containsKey(new Token(current.toUpperCase(), docID, date, title.contains(current.toUpperCase()), fileName))) {
                    termMap.put(new Token(current.toLowerCase(), docID, date, title.contains(current.toLowerCase()), fileName), termMap.remove(new Token(current.toUpperCase(), docID, date, title.contains(current.toUpperCase()), fileName))); // remove uppercase key and update to lowercase key
                    putTermString(current.toLowerCase(), docID, stemming, date, title);
                    return true;
                } else {
                    putTermString(current.toLowerCase(), docID, stemming, date, title);
                    return true;
                }
            }
            /*else {
                putTermString(current, docID, stemming,date, title);
                return true;
            }*/
        }
        if (stopwords.contains(current.toLowerCase())) {
            String newStopWord = current;
            boolean flag = false;
            boolean allStopwords = true;
            int stopIndex = index;
            if (current.equalsIgnoreCase("between")) {
                if (index + 3 < tokens.size() && tokens.get(index + 2).getStr().equalsIgnoreCase("and") && isNumber(tokens.get(index + 1).getStr()) && isNumber(tokens.get(index + 3).getStr())) {
                    if (index + 4 < tokens.size()) {

                        if (tokens.get(index + 2).getStr().equalsIgnoreCase("and")) {
                            if (isNumber(tokens.get(index + 1).getStr()) && isNumber(tokens.get(index + 3).getStr())) {
                                if (tokens.get(index + 4).getStr().equalsIgnoreCase("million") || tokens.get(index + 4).getStr().equalsIgnoreCase("billion") || tokens.get(index + 4).getStr().equalsIgnoreCase("thousand")) {
                                    putTermString(tokens.get(index + 1).getStr() + "-" + addMeasure(tokens.get(index + 3).getStr(), tokens.get(index + 4).getStr()), docID, stemming, date, title);
                                    return true;
                                } else {
                                    putTermString(tokens.get(index + 1).getStr() + "-" + tokens.get(index + 3).getStr(), docID, stemming, date, title);
                                    return true;
                                }
                            }
                        }
                    } else if (index + 3 < tokens.size()) {
                        putTermString(tokens.get(index + 1).getStr() + "-" + tokens.get(index + 3).getStr(), docID, stemming, date, title);
                        return true;
                    }
                } else if (index + 5 < tokens.size()) {
                    if (tokens.get(index + 3).getStr().equalsIgnoreCase("and")) {
                        if (isNumber(tokens.get(index + 1).getStr()) && isNumber(tokens.get(index + 4).getStr())) {
                            if (tokens.get(index + 2).getStr().equalsIgnoreCase("million") || tokens.get(index + 2).getStr().equalsIgnoreCase("billion")
                                    || tokens.get(index + 2).getStr().equalsIgnoreCase("thousand")) {
                                if (tokens.get(index + 5).getStr().equalsIgnoreCase("thousand") || tokens.get(index + 5).getStr().equalsIgnoreCase("million")
                                        || tokens.get(index + 5).getStr().equalsIgnoreCase("billion")) {
                                    putTermString(addMeasure(tokens.get(index + 1).getStr(), tokens.get(index + 2).getStr()) + "-" + addMeasure(tokens.get(index + 4).getStr(), tokens.get(index + 5).getStr()), docID, stemming, date, title);
                                    return true;
                                } else {
                                    putTermString(addMeasure(tokens.get(index + 1).getStr(), tokens.get(index + 2).getStr()) + "-" + tokens.get(index + 4).getStr(), docID, stemming, date, title);
                                    return true;
                                }
                            }
                        }
                    }
                }

            }
            if (Character.isUpperCase(current.charAt(0))) {
                while (stopIndex + 1 < tokens.size() && !flag) {
                    stopIndex = stopIndex + 1;
                    String afterStop = tokens.get(stopIndex).getStr();
                    if (allStopwords && stopwords.contains(afterStop.toLowerCase())) {
                        allStopwords = true;
                    } else {
                        allStopwords = false;
                    }
                    if (Character.isUpperCase(afterStop.charAt(0))) {
                        newStopWord = newStopWord + " " + afterStop;
                    } else {
                        flag = true;
                    }
                }
                if (!newStopWord.equals(current) && !allStopwords && tokens.size() > 0 && newStopWord.split(" ").length < 5) {
                    putTermEntity(newStopWord, docID, date, title);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * this function assist the stringHendler and it putting the term in the term map
     *
     * @param current the token that needed to be handle
     * @param docID   the document id of the term
     * @param stem    if the stemmer option was selected
     * @param date    the date of the document
     * @param title   the title of the document
     */
    private void putTermString(String current, String docID, boolean stem, String date, String title) {
        if (stem == true) {
            porterStemmer porter = new porterStemmer();
            if (Character.isUpperCase(current.charAt(0))) {
                porter.setCurrent(current.toLowerCase());
                porter.stem();
                current = porter.getCurrent().toUpperCase();
            } else {
                porter.setCurrent(current);
                porter.stem();
                current = porter.getCurrent();
            }
        }
        Token currTok = new Token(current, docID, date, title.contains(current), fileName);
        if (termMap.containsKey(currTok)) {
            if (termMap.get(currTok).containsKey(docID)) {
                termMap.get(currTok).get(docID).set(0, String.valueOf(Integer.parseInt(termMap.get(currTok).get(docID).get(0)) + 1));
                updateMaxTf(current, "", docID, date, title);
                updateWordList(current, "");
            } else {
                termMap.get(currTok).put(docID, new ArrayList<>(3));
                termMap.get(currTok).get(docID).add(0, "1");
                termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
                termMap.get(currTok).get(docID).add(2, date);
                updateMaxTf(current, "", docID, date, title);
                updateWordList(current, "");
            }
        } else if (current.length() > 1) {
            termMap.put(currTok, new HashMap<String, ArrayList<String>>());
            termMap.get(currTok).put(docID, new ArrayList<>(3));
            termMap.get(currTok).get(docID).add(0, "1");
            termMap.get(currTok).get(docID).add(1, String.valueOf(Boolean.compare(currTok.isInTitle(), false)));
            termMap.get(currTok).get(docID).add(2, date);
            updateMaxTf(current, "", docID, date, title);
            updateWordList(current, "");
        }

    }

    /**
     * this function check if the string is a valid date
     *
     * @param dateStr the string that needed to be checked
     * @return
     */
    public boolean isValidDate(String dateStr) {
        DateFormat sdf = new SimpleDateFormat("YYYY");
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     * this function that change the format of the number
     *
     * @param current the number
     * @return the string after the format
     * @throws ParseException
     */
    private String format(String current) throws ParseException {
        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        Number number = format.parse(current);
        double d = number.doubleValue();
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%s", d);
    }

    /**
     * this function is a getter that returns the term map
     *
     * @return the term map
     */
    public Map<Token, Map<String, ArrayList<String>>> getTermMap() {
        return termMap;
    }

    /**
     * this function is a setter that set the term map
     *
     * @param termMap the term map that needed to be sets
     */
    public void setTermMap(Map<Token, Map<String, ArrayList<String>>> termMap) {
        this.termMap = termMap;
    }

    /**
     * this function is a getter that gets the map of the max tf
     *
     * @return the map of the max tf
     */
    public Map<String, Integer> getMaxTf() {
        return maxTf;
    }

    public void setMaxTf(Map<String, Integer> maxTf) {
        this.maxTf = maxTf;
    }

    /**
     * this functuib is a getter that get the map of the word counter
     *
     * @return the map of the word counter
     */
    public Map<String, Integer> getWordCounter() {
        return wordCounter;
    }

    /**
     * this function is a setter that set the eordCounter map.
     *
     * @param wordCounter
     */
    public void setWordCounter(Map<String, Integer> wordCounter) {
        this.wordCounter = wordCounter;
    }


}