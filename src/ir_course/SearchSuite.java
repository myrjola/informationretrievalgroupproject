package ir_course;


import java.util.List;

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

    public static final String MORELIKEVSM = "more-like-vsm";
    private static final String[] moreLikeVsmSimple = { corpusPath, MORELIKEVSM, SIMPLE };
    private static final String[] moreLikeVsmPorter = { corpusPath, MORELIKEVSM, PORTER };
    
    public static void main(String[] args) {
        LuceneSearchApp app = new LuceneSearchApp();
        
        runCombination(app, "BM25 vs VSM (both using StandardAnalyzer)", "VSM, BM25", defaults, bm25Standard);
        runCombination(app, "StandardAnalyzer vs SimpleAnalyzer vs Porter stemming", "StandardAnalyzer,SimpleAnalyzer,Porter stemming",defaults, vsmSimple, vsmPorter);
        runCombination(app, "Default vs MoreLikeVSM", "Default,MoreLikeVSM", defaults, moreLikeVsmSimple);
//        runCombination(app, "BM25 vs VSM", defaults, bm25Standard, bm25Porter, bm25Simple, vsmSimple, vsmPorter);
    }
    
    public static void runCombination(LuceneSearchApp app, String label, String legend, String[]... tests) {
        String plotTex = "\\begin{tikzpicture} \n\\begin{axis}[\n\ttitle={";
    	plotTex += label;
    	plotTex += "},\n\txlabel={Recall}, \n\tylabel={Precision},\n\txmin=0, \n\txmax=1,\n\tymin=0, \n\tymax=1,"
    			+ "\n\txtick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tytick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tlegend pos=outer north east,]";
    	
    	System.out.print(plotTex);

    	for( String[] test : tests) {
    		app.main(test);
    	}

    	System.out.print("\n\\legend{"+legend);
    	System.out.print( "}\n\\end{axis} \n\\end{tikzpicture}\n");        
    }
}
