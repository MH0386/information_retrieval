package invertedIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Index {

    // --------------------------------------------
    public int num_files = 0;
    public String[] files;
    public String query;
    public Map<Integer, SourceRecord> sources; // store the doc_id and the file name.
    public HashMap<String, DictEntry> index; // THe inverted index
    public String[] all_unique_words_doc;
    public String[] all_unique_words_query;
    SortedScore sortedScore;
    // --------------------------------------------

    public Index() {
        sources = new HashMap<Integer, SourceRecord>();
        index = new HashMap<String, DictEntry>();
    }

    public void setNum_files(int num_files) {
        this.num_files = num_files;
    }

    public double computeCosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int len = vector1.length;
        for (int i = 0; i < len; i++) {
            dotProduct += vector1[i] * vector2[i];
            normA += Math.pow(vector1[i], 2);
            normB += Math.pow(vector2[i], 2);
        }
        // System.out.println("dotProduct: " + dotProduct);
        // System.out.println("normA: " + normA);
        // System.out.println("normB: " + normB);

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public int get_tf(String doc, String term) {
        int count = 0;
        if (index.get(term) == null) {
            String[] words = doc.split("\\W+");
            for (String word : words) {
                if (word.equals(term)) {
                    count++;
                }
            }
        } else {
            count = index.get(term).pList.dtf;
        }
        return count;
    }

    public int get_df(String term) {
        int df = 0;
        if (index.get(term) == null) {
            for (int file_id : sources.keySet()) {
                String text = get_text_from_doc(file_id);
                if (text.contains(term)) {
                    df++;
                }
            }
        } else {
            df = index.get(term).doc_freq;
        }
        return df;
    }

    String preprocess_text(String text) {
        text = text.replaceAll("[^a-zA-Z0-9 .]", "");
        text = text.replaceAll("(\\d{1,2}\\.\\d\\.\\d)", "\n$1 ");
        text = text.replaceAll("(\\d{1,2}\\.\\d)", "\n$1 ");
        text = text.replaceAll("(\\d{1,2}\\.\\d)\\s(\\.\\d)", "$1$2");
        text = text.replaceAll("(\\d{1,2})([a-zA-Z])", "\n$1 $2");
        return text;
    }

    public double[] compute_vectors(Boolean is_doc, int file_id) {
        Set<String> input_term_set = new HashSet<>();
        Set<String> all_terms_set = new HashSet<>();
        if (is_doc) {
            input_term_set.addAll(Arrays.asList(get_text_from_doc(file_id).split("\\W+")));
            all_terms_set.addAll(Arrays.asList(all_unique_words_query));
            // System.out.println("input_term_set: " + input_term_set);
        } else {
            input_term_set.addAll(Arrays.asList(all_unique_words_query));
        }
        // System.out.println("\n\ninput_term_set: " + input_term_set);
        all_terms_set.addAll(input_term_set);
        all_terms_set.addAll(Arrays.asList(all_unique_words_doc));
        String[] all_terms = all_terms_set.toArray(new String[0]);
        Arrays.sort(all_terms);
        // System.out.println("all_terms length: " + all_terms.length);
        // System.out.println("all_terms: " + Arrays.toString(all_terms));
        int len = all_terms.length;
        double cosine_normalize_value = 0.0;
        double[] tf_idfs = new double[len];
        double[] vectors = new double[len];
        double tf;
        double df;
        double idf;
        double min_value = 0.0001;
        // DecimalFormat df = new DecimalFormat("#.##");
        for (int i = 0; i < len; i++) {
            if (input_term_set.contains(all_terms[i])) {
                // System.out.println(all_terms[i]);
                if (is_doc) {
                    tf = 1 + Math.log10(get_tf(get_text_from_doc(file_id), all_terms[i]) + min_value);
                    tf_idfs[i] = tf;
                    // System.out.println(" tf: " + get_tf(get_text_from_doc(file_id),
                    // all_terms[i]));
                } else {
                    tf = 1 + Math.log10(get_tf(query, all_terms[i]) + min_value);
                    df = get_df(all_terms[i]);
                    if (df == 0) {
                        df = min_value;
                    }
                    idf = Math.log10((num_files / df) + min_value);
                    // tf = Double.valueOf(df.format(tf));
                    // idf = Double.valueOf(df.format(idf));
                    tf_idfs[i] = tf * idf;

                }
            } else {
                tf_idfs[i] = 0;
            }
        }

        for (int i = 0; i < len; i++) {
            cosine_normalize_value += Math.pow(tf_idfs[i], 2.0);
        }
        cosine_normalize_value = Math.sqrt(cosine_normalize_value);
        for (int i = 0; i < len; i++) {
            vectors[i] = tf_idfs[i] / cosine_normalize_value;
            // vectors[i] = Double.valueOf(df.format(vectors[i]));
        }
        // System.out.println(Arrays.toString(tf_idfs));
        // System.out.println(Arrays.toString(vectors));
        return vectors;
    }

    public String get_text_from_doc(int doc_id) {
        if (sources.containsKey(doc_id)) {
            String file = sources.get(doc_id).URL;
            String text = "";
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    text += line + " ";
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + file);
            }
            return preprocess_text(text.toLowerCase());
        } else {
            return "Document not found";
        }
    }

    public void get_all_unique_words() {
        Set<String> all_terms_in_doc = new HashSet<>();
        Set<String> all_terms_in_query = new HashSet<>();
        query = preprocess_text(query);
        String[] words_in_query = query.toLowerCase().split("\\W+");
        for (String term : index.keySet()) {
            all_terms_in_doc.add(term);
        }
        for (String term : words_in_query) {
            all_terms_in_query.add(term);
        }
        all_unique_words_doc = all_terms_in_doc.toArray(new String[0]);
        all_unique_words_query = all_terms_in_query.toArray(new String[0]);
        Arrays.sort(all_unique_words_doc);
        Arrays.sort(all_unique_words_query);
    }

    public void top_k(int k) {
        System.out.println("\n------------------------- top_k START -------------------------");
        double[] query_vector = compute_vectors(false, -1);
        // System.out.println("query_vec: " + Arrays.toString(query_vector));
        double[] cosine_similarity = new double[num_files];
        for (int i = 0; i < num_files; i++) {
            double[] document_vector = compute_vectors(true, i);
            // System.out.println("doc_vec: " + Arrays.toString(document_vector));
            cosine_similarity[i] = computeCosineSimilarity(query_vector, document_vector);
        }
        sortedScore = new SortedScore();

        for (int i = 0; i < num_files; i++) {
            sortedScore.insertScoreRecord(
                    cosine_similarity[i],
                    sources.get(i).URL,
                    sources.get(i).title,
                    sources.get(i).text);
        }
        sortedScore.printScores(k);
        System.out.println("------------------------- top_k END -------------------------");
    }

    public void searchLoop() {
        String phrase;
        do {
            System.out.print("Print search phrase: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                phrase = in.readLine();
                query = phrase.toLowerCase();
                System.out.println("\tSending -> " + query);
                get_all_unique_words();
                Set<String> all_unique_words = new HashSet<>();
                all_unique_words.addAll(Arrays.asList(all_unique_words_doc));
                all_unique_words.addAll(Arrays.asList(all_unique_words_query));
                System.out.println("\tNumber of unique words: " + all_unique_words.size());
                top_k(10);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (!phrase.isEmpty());

    }

    // ---------------------------------------------
    public void web_crawler(String url) {
        int doc_id = 8;
        Document doc_main;
        Elements all_links = null;
        try {
            doc_main = Jsoup
                    .connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .get();
            Element DOC_PAGE = doc_main.body();
            all_links = DOC_PAGE.select("a[href]");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        if (all_links != null) {
            for (Element link : all_links) {
                try {
                    Document doc_link = Jsoup
                            .connect(link.attr("abs:href"))
                            .userAgent(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                            .get();
                    Element PAGE = doc_link.body();
                    String PAGE_TITLE = doc_link.title();
                    String PAGE_TEXT = PAGE.text();
                    // preprocess the text
                    PAGE_TEXT = PAGE_TEXT.replaceAll("[^a-zA-Z0-9 .]", "");
                    PAGE_TEXT = PAGE_TEXT.replaceAll("(\\d{1,2}\\.\\d\\.\\d)", "\n$1 ");
                    PAGE_TEXT = PAGE_TEXT.replaceAll("(\\d{1,2}\\.\\d)", "\n$1 ");
                    PAGE_TEXT = PAGE_TEXT.replaceAll("(\\d{1,2}\\.\\d)\\s(\\.\\d)", "$1$2");
                    PAGE_TEXT = PAGE_TEXT.replaceAll("(\\d{1,2})([a-zA-Z])", "\n$1 $2");
                    String TEXT = PAGE_TITLE + "\n" + PAGE_TEXT;
                    // save the text to a file
                    String filename = "p" + doc_id;
                    filename = "src\\collection\\" + filename;
                    try (PrintWriter output = new PrintWriter(new FileWriter(filename))) {
                        output.write(TEXT);
                        doc_id++;
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }

        } else {
            System.err.println("Error: Main Link can not be reached");
        }
        System.out.println("Total number of files: " + doc_id + "\n\n");
    }

    // ---------------------------------------------
    public void printPostingList(Posting p) {
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
    @SuppressWarnings("unlikely-arg-type")
    public void buildIndex() {
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
    }

    // ----------------------------------------------------------------------------
    Posting intersect(Posting pL1, Posting pL2) {
        Posting answer = null;
        Posting last = null;
        while (pL1 != null && pL2 != null) {
            if (pL1.docId == pL2.docId) {
                if (answer == null) {
                    answer = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = answer;
                } else {
                    last.next = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = last.next;
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
    @SuppressWarnings("unlikely-arg-type")
    public void buildBiwordIndex() {
        int file_id = 0;
        for (String file_name : files) {
            List<String> WORDS = new ArrayList<>();
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
                        WORDS.add(word.toLowerCase());
                    }
                }
                for (int i = 0; i < WORDS.size() - 1; i++) {
                    String word = WORDS.get(i) + "_" + WORDS.get(i + 1);
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
                sources.get(file_id).length = file_length;
            } catch (IOException e) {
                System.out.println("File " + file_name + " not found. Skip it");
            }
            file_id++;
        }
    }

    // ----------------------------------------------------------------------------
    public List<List<Integer>> positional_list(String Word) {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(new ArrayList<>());
        }
        Posting posting = index.get(Word).pList;
        while (posting != null) {
            posting.docId += 1;
            List<String> WORDS = new ArrayList<>();
            try (BufferedReader file = new BufferedReader(new FileReader("src\\collection\\p" + posting.docId))) {
                String ln;
                while ((ln = file.readLine()) != null) {
                    String[] words = ln.split("\\W+");
                    for (String word : words) {
                        WORDS.add(word.toLowerCase());
                    }
                }
                for (int i = 0; i < WORDS.size(); i++) {
                    if (WORDS.get(i).equals(Word)) {
                        result.get(posting.docId - 1).add(i + 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            posting = posting.next;
        }
        return result;
    }

    // ----------------------------------------------------------------------------
    public boolean is_sequential_list(List<Integer> l1, List<Integer> l2) {
        for (int i = 0; i < l2.size(); i++) {
            for (int j = 0; j < l1.size(); j++) {
                if (l2.get(i) == l1.get(j) + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public String positionalIndex(String phrase) {
        String result = "";
        String[] words = phrase.split("\\W+");
        if (words.length < 2) {
            return "Please enter at least two words\n";
        }
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toLowerCase();
        }
        System.out.println("Searching for: " + Arrays.toString(words) + " with length: " + words.length);
        List<List<List<Integer>>> all_positions = new ArrayList<>();
        List<List<Integer>> pairs;
        List<Integer> common_doc = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            all_positions.add(new ArrayList<>());
        }
        for (int i = 0; i < words.length; i++) {
            all_positions.get(i).addAll(positional_list(words[i]));
        }
        for (int j = 0; j < 7; j++) {
            pairs = new ArrayList<>();
            for (int i = 0; i < words.length; i++) {
                pairs.add(all_positions.get(i).get(j));
            }
            if (is_sequential_list(pairs.get(0), pairs.get(1))) {
                common_doc.add(j);
            }
            // System.out.println(pairs);
        }
        // for (int i = 0; i < all_positions.size(); i++) {
        // for (int j = 0; j < all_positions.get(i).size(); j++) {
        // System.out.println(all_positions.get(i).get(j));
        // System.out.println();
        // }
        // }
        for (int doc : common_doc) {
            result += "\t" + doc + " - " + sources.get(doc).title + " - " + sources.get(doc).length + "\n";
        }
        return result;
    }

    // ----------------------------------------------------------------------------
    public int indexOneLine(String ln, int fid) {
        int flen = 0;

        String[] words = ln.split("\\W+");
        // String[] words = ln.replaceAll("(?:[^a-zA-Z0-9 -]|(?<=\\w)-(?!\\S))",
        // "").toLowerCase().split("\\s+");
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
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toLowerCase();
        }
        System.out.println("Searching for: " + Arrays.toString(words) + " with length: " + words.length);
        int len = words.length;
        Posting posting = null;
        try {
            posting = index.get(words[0]).pList;
        } catch (NullPointerException e) {
            return "No results found\n";
        }
        try {
            for (int i = 1; i < len; i++) {
                posting = intersect(posting, index.get(words[i]).pList);
            }
        } catch (NullPointerException e) {
            return "No results found\n";
        } catch (Exception e) {
            System.out.println("Invalid Term");
        }
        while (posting != null) {
            result += "\t" + posting.docId + " - " + sources.get(posting.docId).title + " - "
                    + sources.get(posting.docId).length + "\n";
            posting = posting.next;
        }
        return result;
    }

    // ---------------------------------
    public String[] sort(String[] words) { // bubble sort
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
