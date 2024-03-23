package invertedIndex;

public class SourceRecord {
    public int fid;
    public String URL;
    public String title;
    public String text;
    public Double norm;
    public int length;

    public String getURL() {
        return URL;
    }

    public SourceRecord(int file_id, String url, String Title, int Length, Double Norm, String Text) {
        fid = file_id;
        URL = url;
        title = Title;
        text = Text;
        norm = Norm;
        length = Length;
    }

    public SourceRecord(int file_id, String url, String Title, String Text) {
        fid = file_id;
        URL = url;
        title = Title;
        text = Text;
        norm = 0.0;
        length = 0;
    }
}
