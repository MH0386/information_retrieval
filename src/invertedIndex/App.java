package invertedIndex;

import java.io.File;
import java.io.IOException;

public class App {
    public static void main(String args[]) throws IOException {
        Index index = new Index();
        String files = "src\\collection\\";
        File file = new File(files);
        // |** String[] list()
        // |** Returns an array of strings naming the files and directories in the
        // directory denoted by this abstract pathname.
        String[] fileList = file.list();
        fileList = index.sort(fileList);
        index.num_files = fileList.length;

        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = files + fileList[i];
        }
        index.buildIndex(fileList);
//        index.store("index");
//        index.printDictionary();

//        String test3 = "data  should plain greatest comif"; // data should plain greatest comif
//        System.out.println("Boolean Model result = \n" + index.find_24_01(test3));
        Posting p1 = new Posting(1);
        p1.next = new Posting(2);
        p1.next.next = new Posting(3);
        Posting p2 = new Posting(2);
        p2.next = new Posting(3);
        Posting ans = index.intersect(p1, p2);
        while (ans != null) {
            System.out.println(ans.docId);
            ans = ans.next;
        }

//        String phrase = "";
//        do {
//            System.out.println("Print search phrase: ");
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            phrase = in.readLine();
//            /// -3- **** complete here ****
//        } while (!phrase.isEmpty());

    }
}
