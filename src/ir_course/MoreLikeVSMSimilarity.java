package ir_course;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 *
 * @author william
 */
public class MoreLikeVSMSimilarity extends DefaultSimilarity {

    @Override
    public float lengthNorm(FieldInvertState state) {
        return 1f;
    }

    @Override
    public float queryNorm(float sumOfSquaredWeights) {
        return 1f;
    }

    @Override
    public float tf(float freq) {
        return freq;
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float)(Math.log(numDocs/(double)(docFreq)));
    }

    @Override
    public String toString() {
        return "More-Like-VSM-Similarity";
    }
}