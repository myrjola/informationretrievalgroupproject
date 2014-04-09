package ir_course;

/**
 * Created by martin on 4/9/14.
 */
public class SearchSuite {

    public static final String corpusPath = "corpus_part2.xml";
    private static String[] defaults = {corpusPath};
    public static final String BM25 = "bm25";
    private static String[] bm25Standard = {corpusPath, BM25};
    public static final String PORTER = "PORTER";
    private static String[] bm25Porter = {corpusPath, BM25, PORTER};
    private static String[] vsmPorter = {corpusPath, PORTER};
    public static final String SIMPLE = "SIMPLE";
    private static String[] vsmSimple = {corpusPath, SIMPLE};
    private static String[] bm25Simple = {corpusPath, BM25, SIMPLE};
    public static void main(String[] args) {
        LuceneSearchApp app = new LuceneSearchApp();
        app.main(defaults);
        app.main(bm25Standard);
        app.main(bm25Porter);
        app.main(bm25Simple);
        app.main(vsmSimple);
        app.main(vsmPorter);
    }
}
