package common;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import org.ejml.simple.SimpleMatrix;

import java.util.List;
import java.util.Properties;

public class SentimentAnalyzer {

    private final StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, parse, sentiment");
        pipeline = new StanfordCoreNLP(props);
    }

    public double getSentimentClass(List<CoreMap> sentences) {
        double sum = 0;
        int numberOfSentences = 0;
        for (CoreMap sentence : sentences) {
            Tree sentiments = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int predictedClass = RNNCoreAnnotations.getPredictedClass(sentiments);

            if (predictedClass != 2) {
                sum += predictedClass;
                numberOfSentences++;
            }
        }
        if (numberOfSentences == 0)
            return 0;
        return (sum / numberOfSentences - 2) / 2;
    }

    public double getScore(List<CoreMap> sentences, double sentimentType) {
        double sum0 = 0;
        double sum1 = 0;
        double sum3 = 0;
        double sum4 = 0;
        int numberOfSentences = 0;
        for (CoreMap sentence : sentences) {
            Tree sentiments = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int predictedClass = RNNCoreAnnotations.getPredictedClass(sentiments);

            SimpleMatrix matrix = RNNCoreAnnotations.getPredictions(sentiments);


            if (predictedClass != 2) {
                sum0 += matrix.get(0, 0);
                sum1 += matrix.get(1, 0);
                sum3 += matrix.get(3, 0);
                sum4 += matrix.get(4, 0);
                numberOfSentences++;
            }
        }
        double avg0 = sum0 / numberOfSentences;
        double avg1 = sum1 / numberOfSentences;
        double avg3 = sum3 / numberOfSentences;
        double avg4 = sum4 / numberOfSentences;


        if (sentimentType < 0.5) {
            return avg0;
        } else if (sentimentType < 0) {
            return avg1;
        } else if (sentimentType < 0.5) {
            return avg3;
        } else {
            return avg4;
        }
    }

    public List<CoreMap> getAnnotations(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation.get(CoreAnnotations.SentencesAnnotation.class);
    }

}
