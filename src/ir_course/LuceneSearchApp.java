/*
 * Skeleton class for the Lucene search program implementation
 * Created on 2011-12-21
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 *
 * Author for missing functionality: Martin Yrjölä 84086N <martin.yrjola@aalto.fi>
 */
package ir_course;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.FieldComparator.RelevanceComparator;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneSearchApp {

    public static final String INDEXFILE = "index";
    public static final String TITLE = "title";
    private static final String ABSTRACT = "abstract";
    public static final String RELEVANT = "relevant";  

    private static int totalNumRelevantRecords = 0;
    
    public LuceneSearchApp() {

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            Stemmer stemmer = Stemmer.STANDARD;
            // if analyzer defined
            if (args.length > 1) {
                stemmer = Stemmer.valueOf(args[1]);
            }

            engine.index(docs, true, stemmer);

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
            
            inAbstract = new ArrayList<>();
            inAbstract.add("automatically");
            results = engine.search(null, null, inAbstract, null);
            engine.printResults(results);

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

    public void index(List<DocumentInCollection> docs, boolean isTfIdf, Stemmer stemmer) {
        try {
            Directory dir = FSDirectory.open(new File(INDEXFILE));
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
            if (stemmer.equals(Stemmer.PORTER)) {
                analyzer = new PorterAnalyzer();
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
                doc.add(new Field(TITLE, documentInCollection.getTitle(), TextField.TYPE_STORED));
                doc.add(new Field(ABSTRACT, documentInCollection.getAbstractText(), TextField.TYPE_STORED));
                if(documentInCollection.isRelevant() && documentInCollection.getSearchTaskNumber() == 4) {
                	doc.add(new Field(RELEVANT, "true", TextField.TYPE_STORED));
                } else {
                	doc.add(new Field(RELEVANT, "false", TextField.TYPE_STORED));
                }
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
        addTermQueries(inAbstract, query, ABSTRACT, BooleanClause.Occur.SHOULD);
        addTermQueries(notInAbstract, query, ABSTRACT, BooleanClause.Occur.MUST_NOT);

        return collectResults(searcher, query);
    }

    private List<String> collectResults(IndexSearcher searcher, BooleanQuery query) {
        List<String> results = new LinkedList<String>();
        TopScoreDocCollector collector = TopScoreDocCollector.create(50, true);
        try {
            // DefaultSimilarity is subclass of TFIDFSimilarity
            DefaultSimilarity similarity = new DefaultSimilarity();
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
            System.out.print(texplot);

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
            Collections.sort(results);
            for (int i = 0; i < results.size(); i++)
                System.out.println(" " + (i + 1) + ". " + results.get(i));
        } else
            System.out.println(" no results");
     }
 }
