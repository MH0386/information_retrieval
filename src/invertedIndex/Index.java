package invertedIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Index {

    // --------------------------------------------
    int num_files = 0;
    public Map<Integer, SourceRecord> sources; // store the doc_id and the file name.

    public HashMap<String, DictEntry> index; // THe inverted index
    // --------------------------------------------

    public Index() {
        sources = new HashMap<Integer, SourceRecord>();
        index = new HashMap<String, DictEntry>();
    }

    public void setNum_files(int num_files) {
        this.num_files = num_files;
    }

    // ---------------------------------------------
    public void printPostingList(Posting p) {
        // Iterator<Integer> it2 = hset.iterator();
        System.out.print("[");
        while (p != null) {
            System.out.print(p.docId);
            if (p.next != null) {
                System.out.print(", ");
            }
            p = p.next;
        }
        System.out.println("]");
    }

    // ---------------------------------------------
    @SuppressWarnings("rawtypes")
    public void printDictionary() {
        Iterator it = index.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            DictEntry dd = (DictEntry) pair.getValue();
            System.out.print("[ " + pair.getKey() + ", " + dd.doc_freq + " ]\t\t=--> ");
            printPostingList(dd.pList);
        }
        System.out.println("------------------------------------------------------");
        System.out.println("*** Number of terms = " + index.size());

        try (PrintWriter writer = new PrintWriter(new FileWriter("output.txt"))) {
            Iterator it2 = index.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry pair2 = (Map.Entry) it2.next();
                DictEntry dd2 = (DictEntry) pair2.getValue();
                writer.print("[ " + pair2.getKey() + ", " + dd2.doc_freq + " ]\t\t=--> ");
                Posting p = dd2.pList;
                writer.print("[");
                while (p != null) {
                    writer.print(p.docId);
                    if (p.next != null) {
                        writer.print(", ");
                    }
                    p = p.next;
                }
                writer.println("]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // -----------------------------------------------
    public void buildIndex(String[] files) {
        int file_id = 0;
        for (String file_name : files) {
            try (BufferedReader file = new BufferedReader(new FileReader(file_name))) {
                if (!sources.containsKey(file_name)) {
                    sources.put(file_id, new SourceRecord(file_id, file_name, file_name, "notext"));
                }
                String ln;
                int file_length = 0;
                while ((ln = file.readLine()) != null) {
                    String[] words = ln.split("\\W+");
                    file_length += words.length;
                    for (String word : words) {
                        word = word.toLowerCase();
                        if (stopWord(word)) {
                            continue;
                        }
                        // word = stemWord(word);
                        if (!index.containsKey(word)) {
                            index.put(word, new DictEntry());
                        }
                        if (!index.get(word).postingListContains(file_id)) {
                            index.get(word).doc_freq += 1;
                            if (index.get(word).pList == null) {
                                index.get(word).pList = new Posting(file_id);
                                index.get(word).last = index.get(word).pList;
                            } else {
                                index.get(word).last.next = new Posting(file_id);
                                index.get(word).last = index.get(word).last.next;
                            }
                        } else {
                            index.get(word).last.dtf += 1;
                        }
                        index.get(word).term_freq += 1;
                    }
                }
                sources.get(file_id).length = file_length;

            } catch (IOException e) {
                System.out.println("File " + file_name + " not found. Skip it");
            }
            file_id++;
        }
        // printDictionary();
    }

    // ----------------------------------------------------------------------------
    Posting intersect(Posting pL1, Posting pL2) {
        Posting answer = null;
        // Posting last = null;
        while (pL1 != null && pL2 != null) {
            if (pL1.docId == pL2.docId) {
                if (answer == null) {
                    answer = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                } else {
                    answer.next = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                }
                pL1 = pL1.next;
                pL2 = pL2.next;
            } else if (pL1.docId < pL2.docId) {
                pL1 = pL1.next;
            } else {
                pL2 = pL2.next;
            }
        }
        return answer;
    }

    // ----------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void buildBiwordIndex() {
        List<String> WORDS = new ArrayList<>();
        Iterator it = index.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            WORDS.add(pair.getKey().toString());
        }
        for (int i = 0; i < WORDS.size() - 1; i++) {
            String word1 = WORDS.get(i);
            String word2 = WORDS.get(i + 1);
            String biword = word1 + "_" + word2;
            if (!index.containsKey(biword)) {
                index.put(biword, new DictEntry());
            }
            Posting p1 = index.get(word1).pList;
            Posting p2 = index.get(word2).pList;
            Posting p_intersect = intersect(p1, p2);
            // size of p_intersect
            int size = 0;
            Posting p = p_intersect;
            while (p != null) {
                size++;
                p = p.next;
            }
            index.get(biword).doc_freq = size;
            index.get(biword).pList = p_intersect;
            // index.get(biword).last = p_intersect;
        }
    }

    // ----------------------------------------------------------------------------
    public int indexOneLine(String ln, int fid) {
        int flen = 0;

        String[] words = ln.split("\\W+");
        // String[] words = ln.replaceAll("(?:[^a-zA-Z0-9 -]|(?<=\\w)-(?!\\S))", "
        // ").toLowerCase().split("\\s+");
        flen += words.length;
        for (String word : words) {
            word = word.toLowerCase();
            if (stopWord(word)) {
                continue;
            }
            word = stemWord(word);
            // check to see if the word is not in the dictionary
            // if not add it
            if (!index.containsKey(word)) {
                index.put(word, new DictEntry());
            }
            // add document id to the posting list
            if (!index.get(word).postingListContains(fid)) {
                index.get(word).doc_freq += 1; // set doc freq to the number of doc that contain the term
                if (index.get(word).pList == null) {
                    index.get(word).pList = new Posting(fid);
                    index.get(word).last = index.get(word).pList;
                } else {
                    index.get(word).last.next = new Posting(fid);
                    index.get(word).last = index.get(word).last.next;
                }
            } else {
                index.get(word).last.dtf += 1;
            }
            // set the term_freq in the collection
            index.get(word).term_freq += 1;
            if (word.equalsIgnoreCase("lattice")) {

                System.out.println("  <<" + index.get(word).getPosting(1) + ">> " + ln);
            }

        }
        return flen;
    }

    // ----------------------------------------------------------------------------
    boolean stopWord(String word) {
        if (word.equals("the") ||
                word.equals("to") ||
                word.equals("be") ||
                word.equals("for") ||
                word.equals("from") ||
                word.equals("in") ||
                word.equals("a") ||
                word.equals("into") ||
                word.equals("by") ||
                word.equals("or") ||
                word.equals("and") ||
                word.equals("that")) {
            return true;
        }
        if (word.length() < 2) {
            return true;
        }
        return false;

    }

    // ----------------------------------------------------------------------------
    String stemWord(String word) { // skip for now
        return word;
        // Stemmer s = new Stemmer();
        // s.addString(word);
        // s.stem();
        // return s.toString();
    }

    // ----------------------------------------------------------------------------

    public String find(String phrase) { // any number of terms non-optimized search
        String result = "";
        String[] words = phrase.split("\\W+");
        int len = words.length;

        // fix this if word is not in the hash table will crash (done)
        Posting posting = null;
        try {
            posting = index.get(words[0].toLowerCase()).pList;
        } catch (NullPointerException e) {
            return "No results found\n";
        }
        int i = 1;
        while (i < len) {
            posting = intersect(posting, index.get(words[i].toLowerCase()).pList);
            i++;
        }
        while (posting != null) {
            // System.out.println("\t" + sources.get(num));
            result += "\t" + posting.docId + " - " + sources.get(posting.docId).title + " - "
                    + sources.get(posting.docId).length + "\n";
            posting = posting.next;
        }
        return result;
    }

    // ---------------------------------
    String[] sort(String[] words) { // bubble sort
        boolean sorted = false;
        String sTmp;
        // -------------------------------------------------------
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < words.length - 1; i++) {
                int compare = words[i].compareTo(words[i + 1]);
                if (compare > 0) {
                    sTmp = words[i];
                    words[i] = words[i + 1];
                    words[i + 1] = sTmp;
                    sorted = false;
                }
            }
        }
        return words;
    }

    // ---------------------------------

    public void store(String storageName) {
        try {
            String pathToStorage = "src\\" + storageName;
            Writer wr = new FileWriter(pathToStorage);
            for (Map.Entry<Integer, SourceRecord> entry : sources.entrySet()) {
                // System.out.println("Key = " + entry.getKey() + ", Value = " +
                // entry.getValue().URL + ", Value = " + entry.getValue().title + ", Value = " +
                // entry.getValue().text);
                wr.write(entry.getKey().toString() + ",");
                wr.write(entry.getValue().URL + ",");
                wr.write(entry.getValue().title.replace(',', '~') + ",");
                wr.write(entry.getValue().length + ","); // String formattedDouble = String.format("%.2f", fee );
                wr.write(String.format("%4.4f", entry.getValue().norm) + ",");
                wr.write(entry.getValue().text.replace(',', '~') + "\n");
            }
            wr.write("section2" + "\n");
            Iterator it = index.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                DictEntry dd = (DictEntry) pair.getValue();
                // System.out.print("** [" + pair.getKey() + "," + dd.doc_freq + "] <" +
                // dd.term_freq + "> =--> ");
                wr.write(pair.getKey().toString() + "," + dd.doc_freq + "," + dd.term_freq + ";");
                Posting p = dd.pList;
                while (p != null) {
                    // System.out.print( p.docId + "," + p.dtf + ":");
                    wr.write(p.docId + "," + p.dtf + ":");
                    p = p.next;
                }
                wr.write("\n");
            }
            wr.write("end" + "\n");
            wr.close();
            // System.out.println("=============EBD STORE=============");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================
    public boolean storageFileExists(String storageName) {
        java.io.File f = new java.io.File("src\\" + storageName);
        if (f.exists() && !f.isDirectory())
            return true;
        return false;

    }

    // ----------------------------------------------------
    public void createStore(String storageName) {
        try {
            String pathToStorage = "src\\" + storageName;
            Writer wr = new FileWriter(pathToStorage);
            wr.write("end" + "\n");
            wr.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    // load index from hard disk into memory
    public HashMap<String, DictEntry> load(String storageName) {
        try {
            String pathToStorage = "src\\" + storageName;
            sources = new HashMap<Integer, SourceRecord>();
            index = new HashMap<String, DictEntry>();
            BufferedReader file = new BufferedReader(new FileReader(pathToStorage));
            String ln = "";
            int flen = 0;
            while ((ln = file.readLine()) != null) {
                if (ln.equalsIgnoreCase("section2")) {
                    break;
                }
                String[] ss = ln.split(",");
                int fid = Integer.parseInt(ss[0]);
                try {
                    System.out.println("**>>" + fid + " " + ss[1] + " " + ss[2].replace('~', ',') + " " + ss[3] + " ["
                            + ss[4] + "]   " + ss[5].replace('~', ','));

                    SourceRecord sr = new SourceRecord(fid, ss[1], ss[2].replace('~', ','), Integer.parseInt(ss[3]),
                            Double.parseDouble(ss[4]), ss[5].replace('~', ','));
                    // System.out.println("**>>"+fid+" "+ ss[1]+" "+ ss[2]+" "+ ss[3]+" ["+
                    // Double.parseDouble(ss[4])+ "] \n"+ ss[5]);
                    sources.put(fid, sr);
                } catch (Exception e) {

                    System.out.println(fid + "  ERROR  " + e.getMessage());
                    e.printStackTrace();
                }
            }
            while ((ln = file.readLine()) != null) {
                // System.out.println(ln);
                if (ln.equalsIgnoreCase("end")) {
                    break;
                }
                String[] ss1 = ln.split(";");
                String[] ss1a = ss1[0].split(",");
                String[] ss1b = ss1[1].split(":");
                index.put(ss1a[0], new DictEntry(Integer.parseInt(ss1a[1]), Integer.parseInt(ss1a[2])));
                String[] ss1bx; // posting
                for (int i = 0; i < ss1b.length; i++) {
                    ss1bx = ss1b[i].split(",");
                    if (index.get(ss1a[0]).pList == null) {
                        index.get(ss1a[0]).pList = new Posting(Integer.parseInt(ss1bx[0]), Integer.parseInt(ss1bx[1]));
                        index.get(ss1a[0]).last = index.get(ss1a[0]).pList;
                    } else {
                        index.get(ss1a[0]).last.next = new Posting(Integer.parseInt(ss1bx[0]),
                                Integer.parseInt(ss1bx[1]));
                        index.get(ss1a[0]).last = index.get(ss1a[0]).last.next;
                    }
                }
            }
            System.out.println("============= END LOAD =============");
            // printDictionary();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return index;
    }
}
