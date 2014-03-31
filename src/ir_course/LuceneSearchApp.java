/*
 * Skeleton class for the Lucene search program implementation
 * Created on 2011-12-21
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 *
 * Author for missing functionality: Martin Yrjölä 84086N <martin.yrjola@aalto.fi>
 */
package ir_course;

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LuceneSearchApp {

    public static final String INDEXFILE = "index";
    public static final String TITLE = "title";
    private static final String ABSTRACT = "abstract";

    public LuceneSearchApp() {

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            engine.index(docs, true);

            // TODO: should we search also from title?
            // List<String> inTitle;

            List<String> inAbstract;
            List<String> results;

            /* Queries found in corpus:
             automatic face recognition
             computer vision analysis
             image pattern recognition
             scene analysis
             */
            inAbstract = new ArrayList<>();
            inAbstract.add("automatic");
            inAbstract.add("face");
            inAbstract.add("recognition");
            results = engine.search(null, null, inAbstract, null);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("computer");
            inAbstract.add("vision");
            inAbstract.add("analysis");
            results = engine.search(null, null, inAbstract, null);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("image");
            inAbstract.add("pattern");
            inAbstract.add("recognition");
            results = engine.search(null, null, inAbstract, null);
            engine.printResults(results);

            inAbstract = new ArrayList<>();
            inAbstract.add("scene");
            inAbstract.add("analysis");
            results = engine.search(null, null, inAbstract, null);
            engine.printResults(results);

        } else 
            System.out.println("ERROR: the path of the corpus-file has to be passed as a command line argument.");
    }

    public void index(List<DocumentInCollection> docs, boolean isTfIdf) {
        try {
            Directory dir = new RAMDirectory();
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42, analyzer);
            if (isTfIdf) {
                // DefaultSimilarity is subclass of TFIDFSimilarity
                iwc.setSimilarity(new DefaultSimilarity());
            }
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter w = new IndexWriter(dir, iwc);

            for (DocumentInCollection documentInCollection : docs) {
                Document doc = new Document();
                doc.add(new Field(TITLE, documentInCollection.getTitle(), TextField.TYPE_STORED));
                doc.add(new Field(ABSTRACT, documentInCollection.getAbstractText(), TextField.TYPE_STORED));
                w.addDocument(doc);
            }
            w.close();
        } catch (IOException e) {
            System.err.println("Error creating index!");
            throw new RuntimeException(e);
        }

    }

    public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inAbstract, List<String> notInAbstract) {

        printQuery(inTitle, notInTitle, inAbstract, notInAbstract);

        IndexReader reader;
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(INDEXFILE)));
        } catch (IOException e) {
            System.err.println("Error opening index for searching!");
            throw new RuntimeException(e);
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery query = new BooleanQuery();
        addTermQueries(inTitle, query, TITLE, BooleanClause.Occur.MUST);
        addTermQueries(notInTitle, query, TITLE, BooleanClause.Occur.MUST_NOT);
        addTermQueries(inAbstract, query, ABSTRACT, BooleanClause.Occur.MUST);
        addTermQueries(notInAbstract, query, ABSTRACT, BooleanClause.Occur.MUST_NOT);

        return collectResults(searcher, query);
    }

    private List<String> collectResults(IndexSearcher searcher, BooleanQuery query) {
        List<String> results = new LinkedList<>();
        TopScoreDocCollector collector = TopScoreDocCollector.create(1000, false);
        try {
            // DefaultSimilarity is subclass of TFIDFSimilarity
            DefaultSimilarity similarity = new DefaultSimilarity();
            searcher.setSimilarity(similarity);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            for (ScoreDoc sdoc : hits) {
                int docId = sdoc.doc;
                Document d = searcher.doc(docId);
                results.add(String.format("%s; Score: %s", d.get(TITLE), Double.toString(sdoc.score)));
            }

        } catch (IOException e) {
            System.err.println("Error collecting results!");
            throw new RuntimeException(e);
        }
        return results;
    }

    private void addTermQueries(List<String> termList, BooleanQuery q, String field, BooleanClause.Occur occur) {
        if (termList == null) return;
        for (String title : termList) {
            Term t = new Term(field, title);
            TermQuery tq = new TermQuery(t);
            q.add(tq, occur);
        }
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
