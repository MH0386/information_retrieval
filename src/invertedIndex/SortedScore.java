package invertedIndex;

class ScoreRecord {

    public ScoreRecord next = null;
    double score;
    String URL;
    String title;
    String desc;

    public ScoreRecord(double SCORE, String url, String TITLE, String d) {
        score = SCORE;
        desc = d;
        title = TITLE;
        URL = url;
    }
}

public class SortedScore {
    ScoreRecord start = null;
    void insertScoreRecord(ScoreRecord sr) {
        ScoreRecord current = start;
        ScoreRecord previous = null;
        while (current != null && current.score > sr.score) {
            previous = current;
            current = current.next;
        }
        if (start == current) {
            start = sr;
            sr.next = current;
        } else if (current == null || current.score != sr.score) {
            previous.next = sr;
            sr.next = current;
        }
    }
    // ------------------------------------------------
    void insertScoreRecord(double s, String u, String t, String d) {
        insertScoreRecord(new ScoreRecord(s, u, t, d));
    }
    // ------------------------------------------------
    String printScores() {
        String scores = "";
        ScoreRecord p = start;
        int i = 0;
        while (p != null) {
            i++;
            if (i > 25) {
                return scores;
            }
            String str = "score = " + p.score + " \t " + p.title + "  \t\t" + p.URL;
            scores += str + "<br>\n";
            System.out.println(str);
            p = p.next;
        }
        return scores;
    }

    String printScores(int max) {
        String scores = "";
        ScoreRecord p = start;
        int i = 0;
        while (p != null) {
            i++;
            if (i > 25) {
                return scores;
            }
            if (i > max) {
                return scores;
            }
            String str = "score = " + p.score + " \t " + p.title + "  \t\t" + p.URL;
            scores += str + "<br>\n";
            System.out.println(str);
            p = p.next;
        }
        return scores;
    }
    // sortedScore.insertScoreRecord(scores[i], sTemp +"<BR>\n <p> "+
    // this.doc_text.get(i)+"</p>\n </h3> ");
    String getHTMLScores() {
        String scores = "";
        ScoreRecord p = start;
        int i = 0;
        while (p != null) {
            i++;
            if (i > 25) {
                return scores;
            }

            String str = "<div class=\"result_title\">" + p.title + "</div>";
            str += "<div class=\"result_url\">" + "<a href=\"" + p.URL + "\" > " + p.URL + "</a>" + "</div>";
            str += "<div class=\"result_text\">" + p.desc + "</div>";
            str += "<div class=\"result_stats\">" + String.format("%.3f", p.score) + "</div>";
            scores += str + "<br>\n";
            System.out.println(str);
            p = p.next;
        }
        return scores;
    }
}