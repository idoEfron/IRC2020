package Model;

import snowball.ext.porterStemmer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Parser {

    private Map<Token, Map<String, Integer>> termMap;
    private HashSet<String> stopwords;
    private Map<String, Map<String, Integer>> entities = new HashMap<>();
    private Map<String, String> months;
    private Map<String, String> mass;
    private Map<String, String> electrical;
    private boolean stemming;
    private Map<String, Integer> maxTf;
    private Map<String, Integer> wordCounter;
    private Set<String> termsInDoc;
    private ReadFile rf;
    private Indexer indexer;


    public Parser(boolean stem, ReadFile readFile, String stopWordsPath, Indexer indexer) throws IOException, ParseException {
        this.indexer = indexer;
        rf = readFile;
        wordCounter = new HashMap<>();
        termsInDoc = new HashSet<>();
        stemming = stem;
        maxTf = new HashMap<>();
        termMap = new HashMap<>();
        stopwords = new HashSet<String>();
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
            File stopWordsFile = new File(stopWordsPath + "/stopwords.txt");/****************////////////////
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
     * @param docList
     */
    public void parseDocs(String[] docList) throws ParseException, IOException, InterruptedException {
        String docNo = "";
        String title = "";
        String date = "";
        for (int i = 0; i < docList.length; i++) {
            if (!docList[i].equals("\n") && !docList[i].equals("\n\n\n") && !docList[i].equals("\n\n\n\n") && !docList[i].equals("\n\n")) {
                String docId = docList[i];

                try {
                    docNo = docId.substring(docId.indexOf("<DOCNO>") + 8, docId.indexOf("</DOCNO>") - 1);
                    if (docId.contains("<TI>")) {
                        title = docId.substring(docId.indexOf("<TI>") + 10, docId.indexOf("</TI>"));
                    }
                    if (docId.contains("<DATE1>")) {
                        date = docId.substring(docId.indexOf("<DATE1>") + 10, docId.indexOf("</DATE1>") - 1);
                        String[] dateSplit = date.split(" ");
                        if (dateSplit.length == 3 && months.containsKey(dateSplit[1])) {
                            dateSplit[1] = months.get(dateSplit[1]);
                            date = dateSplit[0] + "/" + dateSplit[1] + "/" + dateSplit[2];
                        }

                    }
                } catch (Exception e) {
                    System.out.println("problem in: " + docNo);
                }

                if (docId.contains("<TEXT>") && docId.contains("</TEXT>")) {
                    String txt = "";
                    try {
                        txt = docId.substring(docId.indexOf("<TEXT>") + 7, docId.indexOf("</TEXT>") - 2);
                    } catch (StringIndexOutOfBoundsException e) {
                        System.out.println("problem in file: " + docNo);
                    }
                    txt = txt.replaceAll("[\\<*?\\>|\\p{Ps}|\\p{Pe}]", " ");
                    txt = txt.replace("--", " ");
                    String[] tokens = txt.split("\\s+|\n");
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
                                    afterCleaning.add(new Token(token, docNo, date, title.contains(token), rf.getSubFolder().get(0).getName()));
                                }//token,token.length(),docList.get(i).indexOf(token)
                            }

                        } else if (!currToken.contains("U.S") && !isNumber(currToken) && !(currToken.contains("%") || currToken.contains("$"))
                                && !currToken.matches("[a-zA-Z0-9]a-zA-Z0-9]*")) {
                            if (isNumric(currToken) == false) {
                                String[] afterRemoving = currToken.split("\\W");
                                if (afterRemoving.length > 1) {
                                    if (afterRemoving.length == 2 && (isNumric(afterRemoving[0]) && isNumric(afterRemoving[1])) ||
                                            (isNumric(afterRemoving[1]) && afterRemoving[0].equals("") && afterRemoving[1].length() + 1 == currToken.length()) ||
                                            (isNumric(afterRemoving[0]) && afterRemoving[1].contains("m"))) {
                                        afterCleaning.add(new Token(currToken, docNo, date, title.contains(currToken), rf.getSubFolder().get(0).getName()));
                                    } else {
                                        for (int j = 0; j < afterRemoving.length; j++) {
                                            token = cleanToken(afterRemoving[j]);
                                            if (token.length() > 0) {
                                                afterCleaning.add(new Token(token, docNo, date, title.contains(token), rf.getSubFolder().get(0).getName()));
                                            }//token,token.length(),docList.get(i).indexOf(token)
                                        }
                                    }
                                } else if (afterRemoving.length == 1) {
                                    token = cleanToken(afterRemoving[0]);
                                    afterCleaning.add(new Token(token, docNo, date, title.contains(token), rf.getSubFolder().get(0).getName()));
                                }
                            }
                        } else {
                            token = cleanToken(tokens[y]);
                            if (token.length() > 0) {
                                afterCleaning.add(new Token(token, docNo, date, title.contains(token), rf.getSubFolder().get(0).getName()));
                            }
                        }//bracket on the else
                    }//for on the tokens after split

                    handler(afterCleaning, docNo, date, title);
                }

                wordCounter.put(docNo, termsInDoc.size());
                termsInDoc.clear();
            }

        }//bracket on the for on the doc list's

        stopwords.clear();
        months.clear();
        mass.clear();
        electrical.clear();
        int k = 0;
        indexer.addBlock(this);
    }

    private boolean isNumric(String currToken) {
        try {
            Double.parseDouble(currToken);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

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
                        rangeList.add(new Token(strArray[k], docID, date, inTitle, rf.getSubFolder().get(0).getName()));
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
     * @param token
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
    //checks if the input number is indeed a number
    public boolean isNumber(String str) throws ParseException {
        if (str.length() > 0 && Character.isDigit(str.charAt(0))) {
            Pattern pattern = Pattern.compile("\\d+(,\\d+)*(\\.\\d+)?");
            if (str.matches("\\d+(,\\d+)*(\\.\\d+)?")) {

                return true;
            }
        }

        return false;
    }


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
            } else if (!current.contains("$") && !afterTwo.equals("U.S") && !afterThree.equals("dollars") && (after.contains("Million") || after.contains("Million".toLowerCase()) || after.contains("Million".toUpperCase()))) {
                putTerm(current, "M", docID, date, title);
            } else if (!afterThree.equals("dollars") && !afterTwo.equals("U.S") && !current.contains("$") && (after.contains("Billion") || after.contains("Billion".toLowerCase()) || after.contains("Billion".toUpperCase()))) {
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
                        current.contains("$") || (after.equals("billion") && afterTwo.equals("U.S")
                        && afterThree.equals("dollars")) || (after.equals("million") && afterTwo.equals("U.S"))
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
                    else if (isNumber(current) && after.equals("billion") && afterTwo.equals("U.S") && afterThree.equals("dollars")) {
                        putTerm(current + "000", " M Dollars", docID, date, title);
                        return true;
                    } else if (isNumber(current) && after.equals("million") && afterTwo.equals("U.S") && afterThree.equals("dollars")) {
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
                        Token currToken = new Token(current, docID, date, inTitle, rf.getSubFolder().get(0).getName());
                        if (termMap.containsKey(currToken)) {
                            if (termMap.get(currToken).containsKey(docID)) {
                                termMap.get(currToken).put(docID, termMap.get(currToken).remove(docID) + 1);
                                return true;

                            } else {
                                termMap.get(currToken).put(docID, 1);
                                return true;

                            }
                        } else {
                            termMap.put(currToken, new HashMap<String, Integer>());
                            termMap.get(currToken).put(docID, 1);
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
        }
        return false;
    }

    /**
     * ido create put term
     *
     * @param current
     * @param character
     * @param docId
     */
    private void putTerm(String current, String character, String docId, String date, String title) {
        boolean inTitle = title.contains(current + character);
        Token currToken = new Token(current + character, docId, date, inTitle, rf.getSubFolder().get(0).getName());
        if (termMap.containsKey(currToken)) {
            if (termMap.get(currToken).containsKey(docId)) {
                termMap.get(currToken).put(docId, termMap.get(currToken).remove(docId) + 1);
                updateMaxTf(current, character, docId, date, title);
                updateWordList(current, character);

            } else {
                termMap.get(currToken).put(docId, 1);
                updateMaxTf(current, character, docId, date, title);
                updateWordList(current, character);
            }

        } else {
            termMap.put(currToken, new HashMap<String, Integer>());
            termMap.get(currToken).put(docId, 1);
            updateMaxTf(current, character, docId, date, title);
            updateWordList(current, character);
        }
    }

    public void updateMaxTf(String current, String character, String docID, String date, String title) {
        if (maxTf.containsKey(docID)) {
            maxTf.put(docID, Math.max(termMap.get(new Token(current + character, docID, date, title.contains(current + character), rf.getSubFolder().get(0).getName())).get(docID), maxTf.get(docID)));

        } else {
            maxTf.put(docID, termMap.get(new Token(current + character, docID, date, title.contains(current + character), rf.getSubFolder().get(0).getName())).get(docID));
        }
    }

    public void updateWordList(String current, String character) {
        if (!termsInDoc.contains(current + character)) {
            termsInDoc.add(current + character);
        }

    }

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
                        Token currTok = new Token(months.get(current) + "-" + after, docID, date, title.contains(months.get(current) + "-" + after), rf.getSubFolder().get(0).getName());
                        if (termMap.containsKey(currTok)) {
                            if (termMap.get(currTok).containsKey(docID)) {
                                termMap.get(currTok).put(docID, termMap.get(currTok).get(docID) + 1);
                            } else {
                                termMap.get(currTok).put(docID, 1);
                            }

                        } else {
                            termMap.put(currTok, new HashMap<String, Integer>());
                            termMap.get(currTok).put(docID, 1);
                        }

                    } else if (num > 1900 && isValidDate(after)) {
                        Token currTok = new Token(after + "-" + months.get(current), docID, date, title.contains(after + "-" + months.get(current)), rf.getSubFolder().get(0).getName());
                        if (months.containsKey(currTok)) {
                            if (termMap.get(currTok).containsKey(docID)) {
                                termMap.get(currTok).put(docID, termMap.get(currTok).get(docID) + 1);
                            } else {
                                termMap.get(currTok).put(docID, 1);
                            }

                        } else {
                            termMap.put(currTok, new HashMap<String, Integer>());
                            termMap.get(currTok).put(docID, 1);
                        }
                    }

                }
            } catch (NumberFormatException e) {
                //term is not a date
            }
            /***lower/upper**////
            if (Character.isUpperCase(current.charAt(0))) {
                checkEntity(tokens, index, docID, date, title, rf.getSubFolder().get(0).getName());
                Token currTok = new Token(current.toLowerCase(), docID, date, title.contains(current.toLowerCase()), rf.getSubFolder().get(0).getName());
                if (termMap.containsKey(currTok.getStr().toLowerCase())) {
                    putTermString(current.toLowerCase(), docID, stemming, date, title);
                } else if (termMap.containsKey(new Token(current.toUpperCase(), docID, date, title.contains(current.toUpperCase()), rf.getSubFolder().get(0).getName()))) {
                    putTermString(current.toUpperCase(), docID, stemming, date, title);
                } else {
                    putTermString(current.toUpperCase(), docID, stemming, date, title);
                    return true;
                }
            } else if (Character.isLowerCase(current.charAt(0))) {
                if (termMap.containsKey(new Token(current.toUpperCase(), docID, date, title.contains(current.toUpperCase()), rf.getSubFolder().get(0).getName()))) {
                    termMap.put(new Token(current.toLowerCase(), docID, date, title.contains(current.toLowerCase()), rf.getSubFolder().get(0).getName()), termMap.remove(new Token(current.toUpperCase(), docID, date,title.contains(current.toUpperCase()), rf.getSubFolder().get(0).getName()))); // remove uppercase key and update to lowercase key
                    putTermString(current.toLowerCase(), docID, stemming,date, title);
                    return true;
                } else {
                    putTermString(current.toLowerCase(), docID, stemming, date,title);
                    return true;
                }
            } /*else {
                putTermString(current, docID, stemming, title.contains(current));
                return true;
            }*/
        }
        if (stopwords.contains(current.toLowerCase()) && Character.isUpperCase(current.charAt(0))) {
            String newStopWord = current;
            boolean flag = false;
            boolean allStopwords = true;
            int stopIndex = index;
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
            if (!newStopWord.equals(current) && !allStopwords) {
                putTermString(newStopWord, docID, stemming, date, title);
                return true;
            }
        }
        return false;
    }


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
                if (entities.containsKey(entity.toUpperCase())) {

                    if (entities.get(entity.toUpperCase()).containsKey(docID)) {
                        entities.get(entity.toUpperCase()).put(docID, entities.get(entity.toUpperCase()).get(docID) + 1);
                    } else {
                        entities.get(entity.toUpperCase()).put(docID, 1);
                    }
                    if (this.indexer.getTermDictionary().containsKey(entity.toUpperCase())) {
                        termMap.put(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName), entities.remove(entity.toUpperCase()));
                    } else if (entities.get(entity.toUpperCase()).size() >= 2) {
                        termMap.put(new Token(entity.toUpperCase(), docID, date, title.contains(entity), fileName), entities.remove(entity.toUpperCase()));
                    }
                } else {
                    if (entity.split("[-:, ]").length > 1) {
                        entities.put(entity.toUpperCase(), new HashMap<>());
                        entities.get(entity.toUpperCase()).put(docID, 1);
                    }
                }
            }
        }
    }

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
        Token currTok = new Token(current, docID, date, title.contains(current), rf.getSubFolder().get(0).getName());
        if (termMap.containsKey(currTok)) {
            if (termMap.get(currTok).containsKey(docID)) {
                termMap.get(currTok).put(docID, termMap.get(currTok).remove(docID) + 1);
                updateMaxTf(current, "", docID, date, title);
                updateWordList(current, "");
            } else {
                termMap.get(currTok).put(docID, 1);
                updateMaxTf(current, "", docID, date, title);
                updateWordList(current, "");
            }
        } else if (current.length() > 1) {
            termMap.put(currTok, new HashMap<>());
            termMap.get(currTok).put(docID, 1);
            updateMaxTf(current, "", docID, date, title);
            updateWordList(current, "");
        }

    }

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
     * ido add this function that change the format of the number
     *
     * @param current
     * @return
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

    public Map<Token, Map<String, Integer>> getTermMap() {
        return termMap;
    }

    public void setTermMap(Map<Token, Map<String, Integer>> termMap) {
        this.termMap = termMap;
    }

    public Map<String, Integer> getMaxTf() {
        return maxTf;
    }

    public void setMaxTf(Map<String, Integer> maxTf) {
        this.maxTf = maxTf;
    }

    public Map<String, Integer> getWordCounter() {
        return wordCounter;
    }

    public void setWordCounter(Map<String, Integer> wordCounter) {
        this.wordCounter = wordCounter;
    }


}