package com.hazelcast.jet.demo.support;

import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.SourceBuilder.TimestampedSourceBuffer;
import com.hazelcast.jet.pipeline.StreamSource;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.hazelcast.util.ExceptionUtil.rethrow;

public class TwitterSource {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
    private final ArrayList<String> buffer = new ArrayList<>();
    private final BasicClient client;

    private TwitterSource(Properties properties, List<String> terms) {
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint().trackTerms(terms);

        String consumerKey = properties.getProperty("consumerKey");
        String consumerSecret = properties.getProperty("consumerSecret");
        String token = properties.getProperty("token");
        String tokenSecret = properties.getProperty("tokenSecret");

        if (isMissing(consumerKey) || isMissing(consumerSecret) || isMissing(token) || isMissing(tokenSecret)) {
            throw new IllegalArgumentException("Twitter credentials are missing!");
        }

        Authentication auth = new OAuth1(consumerKey, consumerSecret, token, tokenSecret);
        client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();
        client.connect();
    }

    public static StreamSource<String> twitterSource(List<String> coinMarkers) {
        Properties properties = loadProperties();
        return SourceBuilder.timestampedStream("twitter", ignored -> new TwitterSource(properties, coinMarkers))
                            .fillBufferFn(TwitterSource::addToBuffer)
                            .destroyFn(TwitterSource::destroy)
                            .build();
    }

    private void addToBuffer(TimestampedSourceBuffer<String> sourceBuffer) {
        // drain everything at once for optimal performance and also naturally limit batch size
        queue.drainTo(buffer);
        for (String tweet : buffer) {
            JSONObject object = new JSONObject(tweet);
            if (object.has("text") && object.has("timestamp_ms")) {
                String text = object.getString("text");
                long timestamp = object.getLong("timestamp_ms");
                sourceBuffer.add(text, timestamp);
            }
        }
        buffer.clear();
    }

    private void destroy() {
        if (client != null) {
            client.stop();
        }
    }

    private static boolean isMissing(String test) {
        return test.isEmpty() || "REPLACE_THIS".equals(test);
    }

    private static Properties loadProperties() {
        Properties tokens = new Properties();
        try {
            tokens.load(Thread.currentThread().getContextClassLoader()
                              .getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            throw rethrow(e);
        }
        return tokens;
    }
}
