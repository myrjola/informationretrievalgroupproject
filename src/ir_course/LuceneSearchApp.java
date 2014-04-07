/*
 * Skeleton class for the Lucene search program implementation
 * Created on 2011-12-21
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 *
 * Author for missing functionality: Martin Yrjölä 84086N <martin.yrjola@aalto.fi>
 */
package ir_course;

import com.google.common.base.Joiner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

import java.util.Collections;
import java.util.StringTokenizer;

public class LuceneSearchApp {

    public static final String INDEXFILE = "index";
    public static final String TITLE = "title";
    public static final String RELEVANT = "relevant";
    public static final PorterStemmer PORTER_STEMMER = new PorterStemmer();
    private static final String ABSTRACT = "abstract";
    private static Stemmer stemmer;

    private static int totalNumRelevantRecords = 0;
    
    public LuceneSearchApp() {

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            List<String> argList = Arrays.asList(args);


            stemmer = Stemmer.STANDARD;
            // if analyzer defined
            if (argList.contains("PORTER")) {
                stemmer = Stemmer.PORTER;
            }

            engine.index(docs, true);

            List<String> inTitle;
            List<String> inAbstract;
            List<String> results;

            /* Queries found in corpus:
             automatic face recognition, relevant documents: 50
             computer vision analysis, relevant documents: 21
             image pattern recognition, relevant documents: 37
             scene analysis, relevant documents: 32
             	sum total relevant documents = 140
             */
            
            totalNumRelevantRecords = 140;
            
            Similarity similarity;
            if (argList.contains("bm25")) {
                similarity = new BM25Similarity();
            } else {
                similarity = new DefaultSimilarity();
            }

            inAbstract = new ArrayList<>();
            inAbstract.add("automatically");
            results = engine.search(null, inAbstract, similarity);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("automatic");
            inAbstract.add("face");
            inAbstract.add("recognition");
            results = engine.search(null, inAbstract, similarity);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("computer");
            inAbstract.add("vision");
            inAbstract.add("analysis");
            results = engine.search(null, inAbstract, similarity);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("image");
            inAbstract.add("pattern");
            inAbstract.add("recognition");
            results = engine.search(null, inAbstract, similarity);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("scene");
            inAbstract.add("analysis");
            results = engine.search(null, inAbstract, similarity);
            engine.printResults(results);

        } else 
            System.out.println("ERROR: the path of the corpus-file has to be passed as a command line argument.");
    }

    public void index(List<DocumentInCollection> docs, boolean isTfIdf) {
        try {
            Directory dir = FSDirectory.open(new File(INDEXFILE));
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
            if (stemmer.equals(Stemmer.PORTER)) {
                // Prevent wrong analyzation of stemmed words.
                analyzer = new StopAnalyzer(Version.LUCENE_42);
            }
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42, analyzer);
            if (isTfIdf) {
                // DefaultSimilarity is subclass of TFIDFSimilarity
            	iwc.setSimilarity(new DefaultSimilarity());
            }
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter w = new IndexWriter(dir, iwc);

            for (DocumentInCollection documentInCollection : docs) {
                Document doc = new Document();
                if(documentInCollection.isRelevant() && documentInCollection.getSearchTaskNumber() == 4) {
                	doc.add(new Field(RELEVANT, "true", TextField.TYPE_STORED));
                } else {
                	doc.add(new Field(RELEVANT, "false", TextField.TYPE_STORED));
                }
                doc.add(new Field(TITLE, documentInCollection.getTitle(), TextField.TYPE_STORED));

                String abstractString = documentInCollection.getAbstractText();
                if (stemmer.equals(Stemmer.PORTER)) {
                    StringTokenizer tokenizer = new StringTokenizer(abstractString);
                    List<String> stemmedList = new LinkedList<>();
                    while (tokenizer.hasMoreTokens()) {
                        stemmedList.add(porterStem(tokenizer.nextToken().toLowerCase()));
                    }
                    Joiner joiner = Joiner.on(" ").skipNulls();
                    abstractString = joiner.join(stemmedList);
                }

                doc.add(new Field(ABSTRACT, abstractString, TextField.TYPE_STORED));
                w.addDocument(doc);
            }
            w.close();
        } catch (IOException e) {
            System.err.println("Error creating index!");
            throw new RuntimeException(e);
        }

    }

    public List<String> search(List<String> inTitle, List<String> inAbstract, Similarity similarity) {

        printQuery(inTitle, null, inAbstract, null);

        IndexReader reader;
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(INDEXFILE)));
        } catch (IOException e) {
            System.err.println("Error opening index for searching!");
            throw new RuntimeException(e);
        }

        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery query = new BooleanQuery();
        List<String> relevantlist = new ArrayList<String>();
        relevantlist.add("true");
        addTermQueries(inTitle, query, TITLE, BooleanClause.Occur.MUST);
        addTermQueries(inAbstract, query, ABSTRACT, BooleanClause.Occur.SHOULD);
        query.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
                
        
        return collectResults(searcher, query, similarity);
    }


    private List<String> collectResults(IndexSearcher searcher, BooleanQuery query, Similarity similarity) {
        List<String> results = new LinkedList<>();
        TopScoreDocCollector collector = TopScoreDocCollector.create(1000, false);
        try {
            // DefaultSimilarity is subclass of TFIDFSimilarity
            if (similarity == null) {
                similarity = new DefaultSimilarity();
            }
            searcher.setSimilarity(similarity);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            LinkedList<Document> plotlist = new LinkedList<Document>(); 
            for (ScoreDoc sdoc : hits) {
                int docId = sdoc.doc;
                Document d = searcher.doc(docId);
                results.add(d.get(TITLE) );
                plotlist.add(d);
            }
            /* Run plotter
             * Plotter takes lucene documents, so it must be called from here
             * before the results are converted to List<String> and relevance data is lost.
             * Plotter also needs to know what the total recall is for each query, as the results
             * don't include those relevant documents that the query missed.
             */ 
            Plotter plotter = new Plotter("/tmp/");
            String texplot = plotter.PlotListAsString(plotlist, "testplot", totalNumRelevantRecords);
            System.out.println(texplot);

        } catch (IOException e) {
            System.err.println("Error collecting results!");
            throw new RuntimeException(e);
        }
        return results;
    }

    private void addTermQueries(List<String> termList, BooleanQuery q, String field, BooleanClause.Occur occur) {
        if (termList == null) return;
        for (String termString : termList) {
            if (stemmer.equals(Stemmer.PORTER)) {
                termString = porterStem(termString);
            }
            Term t = new Term(field, termString);
            TermQuery tq = new TermQuery(t);
            q.add(tq, occur);
        }
    }

    private String porterStem(String string) {
        PORTER_STEMMER.setCurrent(string);
        PORTER_STEMMER.stem();
        return PORTER_STEMMER.getCurrent();
    }

    public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inAbstract, List<String> notInAbstract) {
        System.out.print("Search (");
        if (inTitle != null) {
            System.out.print("in title: " + inTitle);
            if (notInTitle != null || inAbstract != null || notInAbstract != null)
                System.out.print("; ");
        }
        if (notInTitle != null) {
            System.out.print("not in title: " + notInTitle);
            if (inAbstract != null || notInAbstract != null)
                System.out.print("; ");
            }
        if (inAbstract != null) {
            System.out.print("in abstract: " + inAbstract);
            if (notInAbstract != null)
                System.out.print("; ");
        }
        if (notInAbstract != null) {
            System.out.print("not in abstract: " + notInAbstract);
        }
        System.out.println("):");
    }


    public void printResults(List<String> results) {
        if (results.size() > 0) {
            for (int i = 0; i < results.size(); i++)
                System.out.println(" " + (i + 1) + ". " + results.get(i));
        } else
            System.out.println(" no results");
     }
 }
