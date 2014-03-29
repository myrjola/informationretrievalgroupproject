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
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LuceneSearchApp {

    public static final String INDEXFILE = "index";
    public static final String TITLE = "title";
    private static final String ABSTRACT = "abstract";
    private static final String RELEVANT = "relevant";

    public LuceneSearchApp() {

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            engine.index(docs);

            List<String> inTitle;
            List<String> notInTitle;
            List<String> inAbstract;
            List<String> notInAbstract;
            List<String> results;

            // 1) search documents with words "kim" and "korea" in the title
            inTitle = new LinkedList<String>();
            inTitle.add("computer");
            inTitle.add("vision");
            results = engine.search(inTitle, null, null, null);
            engine.printResults(results);

            // 2) search documents with word "kim" in the title and no word "korea" in the description
            inTitle = new LinkedList<String>();
            notInAbstract = new LinkedList<String>();
            inTitle.add("kim");
            notInAbstract.add("korea");
            results = engine.search(inTitle, null, null, notInAbstract);
            engine.printResults(results);

            // 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
            inTitle = new LinkedList<String>();
            inTitle.add("us");
            notInTitle = new LinkedList<String>();
            notInTitle.add("dawn");
            inAbstract = new LinkedList<String>();
            inAbstract.add("american");
            inAbstract.add("confession");
            results = engine.search(inTitle, notInTitle, inAbstract, null);
            engine.printResults(results);
        } else
            System.out.println("ERROR: the path of the corpus-file has to be passed as a command line argument.");
    }

    public void index(List<DocumentInCollection> docs) {
        try {
            Directory dir = FSDirectory.open(new File(INDEXFILE));
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42, analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter w = new IndexWriter(dir, iwc);

            for (DocumentInCollection documentInCollection : docs) {
                Document doc = new Document();
                doc.add(new Field(TITLE, documentInCollection.getTitle(), TextField.TYPE_STORED));
                doc.add(new Field(ABSTRACT, documentInCollection.getAbstractText(), TextField.TYPE_STORED));
                String relevance = Boolean.toString(documentInCollection.isRelevant());
                doc.add(new Field(RELEVANT, relevance, TextField.TYPE_STORED));
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
        List<String> results = new LinkedList<String>();
        TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
        try {
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            for (ScoreDoc sdoc : hits) {
                int docId = sdoc.doc;
                Document d = searcher.doc(docId);
                results.add(d.get(TITLE));
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
            Collections.sort(results);
            for (int i = 0; i < results.size(); i++)
                System.out.println(" " + (i + 1) + ". " + results.get(i));
        } else
            System.out.println(" no results");
    }
}
