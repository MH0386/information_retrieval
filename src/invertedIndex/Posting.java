package invertedIndex;

public class Posting {

    public Posting next = null;
    int docId;
    int dtf = 1;

    Posting(int id, int t) {
        docId = id;
        dtf=t;
    }
    
    Posting(int id) {
        docId = id;
    }
}