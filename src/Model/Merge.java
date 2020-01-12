package Model;

import ViewModel.viewModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Merge implements Runnable {

    private static Semaphore mutex = new Semaphore(1);
    File[] directory;
    private boolean isdoc;

    /**
     * this is the constructor of this class
     *
     * @param files
     */
    public Merge(File[] files,boolean isdoc) {
        directory = files;
        this.isdoc = isdoc;
    }

    /**
     * this function merge all the files in the folder to one folder
     *
     * @param files the list of files
     * @throws IOException
     */
    private void merge(File[] files) throws IOException, InterruptedException {
        mutex.acquire();
        Map<String, Map<String, ArrayList<Integer>>> newIndex = Indexer.getTermDictionary();
        List<String> mergedText = new ArrayList<>();
        String parent = files[0].getParent();
        File temp = new File(files[0].getParent());
        //String posting = new File(temp.getParent()).getParent();
        //String postingPath = new File(posting).getParent();

      //  File docEntities = new File(postingPath + "/" + "docEntities.txt");
      // uploadMap(posting,docEntities, newIndex);
        File docInfo = new File( viewModel.getPostPath()+ "/" + "corpusInfo.txt");
        if(!docInfo.exists()) {
            saveNumDocsAndDocLength(viewModel.getPostPath(), ReadFile.getDocs(), Indexer.getTotalDocLength());
        }
        File merged = new File(parent + parent.substring(parent.lastIndexOf('\\')) + "_merged.txt");
        for (File file : files) {
            if (file.isDirectory()) {

                //System.out.println("Directory: " + file.getName());

                merge(file.listFiles()); // Calls same method again.
            } else {
                if (!file.getName().contains("merged.txt")) {
                    List<String> text = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                    mergedText.addAll(text);
                    file.delete();
                }
            }
        }
        if (mergedText != null && mergedText.size() > 0) {
            mergedText.addAll(Files.readAllLines(merged.toPath()));
            Collections.sort(mergedText, String.CASE_INSENSITIVE_ORDER);

            for (int i = mergedText.size() - 1; i > 0; i--) {
                if (mergedText.get(i).contains(":")) {
                    if (mergedText.get(i - 1).contains(":")) {
                        String term = mergedText.get(i).substring(0, mergedText.get(i).indexOf(":"));
                        if (Character.isLowerCase(term.charAt(0))) {
                            if (i > 0) {
                                if (Character.isUpperCase(mergedText.get(i - 1).charAt(0))) {
                                    if (term.toLowerCase().equals(mergedText.get(i - 1).substring(0, mergedText.get(i - 1).indexOf(":")).toLowerCase())) {
                                        String suffix = mergedText.get(i - 1).substring(mergedText.get(i - 1).indexOf(": ") + 2);
                                        mergedText.set(i, mergedText.get(i) + suffix);
                                        mergedText.remove(i - 1);
                                    }
                                } else {
                                    mergeHelper(newIndex, mergedText, i, term);
                                }
                            }
                        } else {
                            if (i > 0) {
                                mergeHelper(newIndex, mergedText, i, term);
                            }
                        }
                    } else {
                        mergedText.remove(i - 1);
                        i++;
                    }

                } else {
                    mergedText.remove(i);
                }

            }

            if(!isdoc){
                documentLines(mergedText, newIndex);
            }
            writeRaw(mergedText, merged.getPath());
            mergedText.clear();
        }
        if(!isdoc){
            Indexer.setTermDictionary(newIndex);
        }
        mutex.release();

    }
    //todo ido add
    private void saveNumDocsAndDocLength(String postingPath, int docs, double totalDocLength) throws IOException, InterruptedException {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(postingPath +"/"+ "corpusInfo.txt",true)));
        writer.append(docs + ">" + totalDocLength);
        writer.flush();
        writer.close();
    }

    //todo ido add
//    private void uploadMap(String posting,File docEntities, Map<String, Map<String, ArrayList<Integer>>> newIndex) throws IOException {
//        if (docEntities.exists()) {
//            List<String> doctxt = Files.readAllLines(docEntities.toPath(), StandardCharsets.UTF_8);
//            docEntities.delete();
//            HashMap<String, Map<String, String>> hashDocEnt = new HashMap<>();
//            for (int i = 0; i < doctxt.size(); i++) {
//                String[] strArr = doctxt.get(i).split(">");
//                hashDocEnt.put(strArr[0], new HashMap<>());
//                String map = strArr[1].replaceAll("\\{", "");
//                map = map.replaceAll("}", "");
//                if (map.contains(",")) {
//                    String[] split = map.split(", ");
//                    for (String s : split) {
//                        try {
//                            if (s.contains("=")) {
//                                String[] value = s.split("=");
//                                hashDocEnt.get(strArr[0]).put(value[0], value[1]);
//                            }
//                        } catch (ArrayIndexOutOfBoundsException e) {
//                            System.out.println(strArr[0]);
//                        }
//                    }
//                }
//            }
//            HashMap<String,Map< String,Set<String>>> docDictionary = new HashMap<>();
//            if (hashDocEnt.size() > 0) {
//                for (String s:hashDocEnt.keySet()) {
//                    hashDocEnt.get(s).keySet().retainAll(newIndex.keySet());
//                    docDictionary.put(s,new HashMap<>());
//                   // selectTopFive(s,postingPath,hashDocEnt.get(s));
//                    docDictionary.put(s,selectTopFive(s,posting,hashDocEnt.get(s)));
//                }
//                Indexer.setDocDictionary(docDictionary);
//            }
//
//        }
//    }
//    public Map<String,Set<String>> selectTopFive(String s,String postingPath, Map<String, String> hashDocEnt){
//        Map<String, String> copyHashDocEnt = new HashMap<>(hashDocEnt);
//        Map<String,Set<String>> topFiveEntitiesDocs = new HashMap<>();
//        if (hashDocEnt.size() > 5) {
//                int numberOfEntities = 0;
//                Set<String> topFive = new HashSet<>();
//                while (numberOfEntities != 5) {
//                    //int max = entitiesPerDoc.get(0);
//                    Set<String> str = copyHashDocEnt.keySet();
//                    String[] strArr = new String[copyHashDocEnt.keySet().size()];
//                    strArr = str.toArray(strArr);
//                    int max = Integer.parseInt(copyHashDocEnt.get(strArr[0]));
//                    String maxString = strArr[0];
//                    for (int k = 1; k < strArr.length; k++) {
//                        if (Integer.parseInt(copyHashDocEnt.get(strArr[k])) > max) {
//                            max = Integer.parseInt(copyHashDocEnt.get(strArr[k]));
//                            maxString = strArr[k];
//                        }
//                    }
//                    copyHashDocEnt.remove(maxString);
//                    topFive.add(maxString);
//                    numberOfEntities++;
//                }
//                topFiveEntitiesDocs.put(postingPath+"/Docs/" + s + ".txt" , topFive);
//            }
//           else if (copyHashDocEnt.size() >= 0)
//            {
//                topFiveEntitiesDocs.put(postingPath+"/Docs/" + s + ".txt", new HashSet<>(copyHashDocEnt.keySet()));
//            }
//           return topFiveEntitiesDocs;
//    }

    private void documentLines(List<String> mergedText, Map<String, Map<String, ArrayList<Integer>>> newIndex) {
        for (int i = 0; i < mergedText.size(); i++) {
            String term = mergedText.get(i).substring(0, mergedText.get(i).indexOf(":") - 1);
            if (newIndex.containsKey(term.toLowerCase())) {
                newIndex.get(term.toLowerCase()).get(newIndex.get(term.toLowerCase()).keySet().toArray()[0]).add(1, i);
            } else if (newIndex.containsKey(term.toUpperCase())) {
                newIndex.get(term.toUpperCase()).get(newIndex.get(term.toUpperCase()).keySet().toArray()[0]).add(1, i);
            } else {
                newIndex.get(term).get(newIndex.get(term).keySet().toArray()[0]).add(1, i);
            }
        }
    }

    /**
     * this is a helper function to concatenate duplicate terms
     *
     * @param mergedText all file lines
     * @param i          index
     * @param term       the term we want to merge its duplicates
     */

    private void mergeHelper(Map<String, Map<String, ArrayList<Integer>>> newIndex, List<String> mergedText, int i, String term) {
        if(!isdoc){
            if (term.toLowerCase().equals(mergedText.get(i - 1).substring(0, mergedText.get(i - 1).indexOf(':')).toLowerCase())) {
                String suffix = mergedText.remove(i).substring(mergedText.get(i - 1).indexOf(": ") + 2);
                mergedText.set(i - 1, mergedText.get(i - 1) + suffix);
                i = i - 1;
            }
        }
    }

    /**
     * this function write in in a text file all the records
     *
     * @param records  the records that needed to be saved
     * @param filePath the file path
     * @throws IOException
     */
    private static void writeRaw(List<String> records, String filePath) throws IOException {
        File file = new File(filePath);
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        write(records, writer);
    }

    /**
     * this function assisting to the writeRaw and saving the file object
     *
     * @param records the recodes list that needed to be saved
     * @param writer  the object that save the object
     * @throws IOException
     */
    private static void write(List<String> records, Writer writer) throws IOException {
        for (String record : records) {
            writer.write(record + '\n');
        }
        writer.flush();
        writer.close();
    }

    /**
     * this fumction run the merge function
     */
    @Override
    public void run() {
        try {
            merge(directory);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
