package com.hazelcast.jet.demo.common;

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

/**
 * Sentiment ingest uses {@link StanfordCoreNLP} class to calculate sentiment scores.
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
        double sentimentType = getSentimentClass(annotations);
        double sentimentScore = getScore(annotations, sentimentType);
        return sentimentType * sentimentScore;
    }

    private double getSentimentClass(List<CoreMap> sentences) {
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
        return numberOfSentences == 0 ? 0 : (sum / numberOfSentences - 2) / 2;
    }

    private double getScore(List<CoreMap> sentences, double sentimentType) {
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
                sum0 += matrix.get(0); // very negative
                sum1 += matrix.get(1); // negative
                sum3 += matrix.get(3); // positive
                sum4 += matrix.get(4); // very positive
                numberOfSentences++;
            }
        }
        double avg0 = sum0 / numberOfSentences;
        double avg1 = sum1 / numberOfSentences;
        double avg3 = sum3 / numberOfSentences;
        double avg4 = sum4 / numberOfSentences;

        if (sentimentType < -0.5) {
            return avg0;
        } else if (sentimentType < 0) {
            return avg1;
        } else if (sentimentType < 0.5) {
            return avg3;
        } else {
            return avg4;
        }
    }

    private List<CoreMap> getAnnotations(String text) {
        Annotation annotation = pipeline.process(text);
        return annotation.get(CoreAnnotations.SentencesAnnotation.class);
    }


}
