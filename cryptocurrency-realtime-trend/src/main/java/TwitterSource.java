import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterSource implements ProcessorSupplier {

    private Properties secret;

    TwitterSource() {
        secret = new Properties();
        try {
            secret.load(this.getClass().getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            //TODO log it here
        }
    }

    @Override
    public Collection<? extends Processor> get(int count) {
        AbstractProcessor abstractProcessor = new AbstractProcessor() {

            private BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);

            private BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(10000);

            @Override
            protected void init(Context context) throws Exception {
                super.init(context);
                StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

                // add some track terms

                List<String> terms = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry:CoinDefs.coinMap.entrySet()){
                    terms.add(entry.getKey());
                    terms.addAll(entry.getValue());
                }
                endpoint.trackTerms(terms);

                String consumerKey = secret.getProperty("consumerKey");
                String consumerSecret = secret.getProperty("consumerSecret");
                String token = secret.getProperty("token");
                String tokenSecret = secret.getProperty("tokenSecret");

                Authentication auth = new OAuth1(consumerKey, consumerSecret, token, tokenSecret);

                Client client = new ClientBuilder()
                        .hosts(Constants.STREAM_HOST)
                        .endpoint(endpoint)
                        .authentication(auth)
                        .processor(new StringDelimitedProcessor(queue))
                        .eventMessageQueue(eventQueue)// optional: use this if you want to process client events
                        .build();

                client.connect();

                Thread thread = new Thread(() -> {
                    while (true) {
                        try {
                            Event take = eventQueue.take();
                            System.out.println(take.getMessage());
                            System.out.println(take.getEventType());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();

            }

            @Override
            public boolean complete() {
                for (int i = 0; i < 100; i++) {
                    String message = queue.poll();
                    if (message != null) {
                        JSONObject jsonObject = new JSONObject(message);
                        String text = jsonObject.getString("text");
                        TimestampedEntry<String, String> timestampedEntry = new TimestampedEntry<>(System.currentTimeMillis(), text.toLowerCase(), "");
//                        System.out.println(">>>> >>>> " + text.toLowerCase());
                        if (!tryEmit(timestampedEntry)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return false;
            }

        };
        return Collections.singleton(abstractProcessor);

    }
}