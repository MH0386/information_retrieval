package invertedIndex;

import java.io.File;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        Index index = new Index();
        String files = "src\\collection\\";
        File file = new File(files);

        String[] fileList = file.list();
        fileList = index.sort(fileList);

        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = files + fileList[i];
        }
        index.num_files = fileList.length;
        index.files = fileList;
        // index.web_crawler("https://en.wikipedia.org/wiki/List_of_pharaohs");
        // index.buildIndex();
        // index.store("index");
        // index.buildBiwordIndex();
        index.load("index");
        // index.printDictionary();
        // String test = "data should plain greatest comif";
        // System.out.println("Boolean Model result = \n" + index.find(test));
        // System.out.println("Boolean Model result = " + index.positionalIndex(test));
        index.top_k("Reinforcement Learning", 10);

        // // Search
        // String phrase = "";
        // do {
        // System.out.print("Search Phrase: ");
        // BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // phrase = in.readLine();
        // if (!phrase.isEmpty()) {
        // if (phrase.contains("\"")) {
        // phrase = phrase.replaceAll("\"(\\w+)\\s+(\\w+)\"", "$1_$2").toLowerCase();
        // }
        // System.out.println("Sending: " + phrase);
        // System.out.println("Result:\n" + index.find(phrase));
        // }
        // } while (!phrase.isEmpty());

        // // Positional Search
        // do {
        // System.out.print("Positional Search Phrase: ");
        // BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // phrase = in.readLine();
        // if (!phrase.isEmpty()) {
        // System.out.println("Sending: " + phrase);
        // System.out.println("Result:\n" + index.positionalIndex(phrase));
        // }
        // } while (!phrase.isEmpty());
    }
}
