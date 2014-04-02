package ir_course;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * Author: myrjola <martin.yrjola@relex.fi>
 * Date: 3/31/14
 */
public class PorterAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer source = new LowerCaseTokenizer(Version.LUCENE_42, reader);
        return new TokenStreamComponents(source, new PorterStemFilter(source));
    }
}
