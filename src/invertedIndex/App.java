package invertedIndex;

import java.io.File;
import java.io.IOException;

public class App {
    public static void main(String args[]) throws IOException {
        Index index = new Index();
        String files = "src\\collection\\";
        File file = new File(files);

        String[] fileList = file.list();
        fileList = index.sort(fileList);
        index.num_files = fileList.length;

        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = files + fileList[i];
        }
        index.buildIndex(fileList);
        index.store("index");
        index.buildBiwordIndex();

        // String test = "data should plain greatest comif";
        // System.out.println("Boolean Model result = \n" + index.find(test));
        // System.out.println("Boolean Model result = " + index.positionalIndex(test));

        // Search
        String phrase = "";
        // do {
        // System.out.println("Print search phrase: ");
        // BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // phrase = in.readLine();
        // if (!phrase.isEmpty()) {
        // System.out.println("Phrase Model result = \n" + index.find(phrase));
        // }
        // } while (!phrase.isEmpty());
        phrase = "\"Reinforcement Learning\"";
        phrase = phrase.replaceAll("\"(\\w+)\\s+(\\w+)\"", "$1_$2").toLowerCase();
        System.out.println("\tSending: " + phrase);
        System.out.println("Phrase Model result = \n" + index.find(phrase));
        // Bi-word search
        // String phrase = "Reinforcement Learning";
        // System.out.println("Positional result \n" + index.positionalIndex(phrase));
    }
}
