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
        // index.printDictionary();

        // String test3 = "data should plain greatest comif"; // data should plain
        // greatest comif
        // System.out.println("Boolean Model result = \n" + index.find(test3));

        // String test4 = "data should plain greatest comif"; // data should plain
        // greatest comif
        // System.out.println("Boolean Model result = " + index.positionalIndex(test4));

        // Posting p1 = new Posting(1);
        // p1.next = new Posting(2);
        // p1.next.next = new Posting(3);
        // Posting p2 = new Posting(2);
        // p2.next = new Posting(3);
        // Posting ans = index.intersect(p1, p2);
        // while (ans != null) {
        // System.out.println(ans.docId);
        // ans = ans.next;
        // }

        // Single word search
        // String phrase = "";
        // do {
        // System.out.println("Print search phrase: ");
        // BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // phrase = in.readLine();
        // if (!phrase.isEmpty()) {
        // System.out.println("Phrase Model result = \n" + index.find(phrase));
        // }
        // } while (!phrase.isEmpty());

        // Bi-word search
        // phrase = "";
        // index.buildBiwordIndex();
        // index.printDictionary();
        String phrase = "should plain";
        System.out.println("Positional result \n" + index.positionalIndex(phrase));

    }
}
