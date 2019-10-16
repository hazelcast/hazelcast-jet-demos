package com.hazelcast.jet.demo.support;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import org.ejml.simple.SimpleMatrix;

import java.util.List;
import java.util.Properties;

/**
 * Uses {@link StanfordCoreNLP} class to calculate sentiment scores.
 */
public class SentimentAnalyzer {

    private final StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, parse, sentiment");
        pipeline = new StanfordCoreNLP(props);
    }

    public double getSentimentScore(String text) {
        List<CoreMap> annotations = getAnnotations(text);
        double overallSentiment = getOverallSentiment(annotations);
        double sentimentScore = getScore(annotations, overallSentiment);
        return overallSentiment * sentimentScore;
    }

    private double getOverallSentiment(List<CoreMap> sentences) {
        double sum = 0;
        int numberOfSentences = 0;
        for (CoreMap sentence : sentences) {
            Tree sentiments = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int predictedClass = RNNCoreAnnotations.getPredictedClass(sentiments);
            if (predictedClass == 2) { // neutral sentiment
                continue;
            }
            sum += predictedClass;
            numberOfSentences++;
        }
        return numberOfSentences == 0 ? 0 : (sum / numberOfSentences - 2) / 2;
    }

    private double getScore(List<CoreMap> sentences, double overallSentiment) {
        int matrixIndex =
                overallSentiment < -0.5  ? 0  // very negative
                : overallSentiment < 0.0 ? 1  // negative
                : overallSentiment < 0.5 ? 3  // positive
                : 4;                       // very positive
        double sum = 0;
        int numberOfSentences = 0;
        for (CoreMap sentence : sentences) {
            Tree sentiments = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int predictedClass = RNNCoreAnnotations.getPredictedClass(sentiments);
            if (predictedClass == 2) { // neutral
                continue;
            }
            SimpleMatrix matrix = RNNCoreAnnotations.getPredictions(sentiments);
            sum += matrix.get(matrixIndex);
            numberOfSentences++;
        }
        return sum / numberOfSentences;
    }

    private List<CoreMap> getAnnotations(String text) {
        return pipeline.process(text)
                       .get(CoreAnnotations.SentencesAnnotation.class);
    }
}
