import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.json.JSONObject;

import static com.hazelcast.util.ExceptionUtil.rethrow;

public class TwitterSource extends AbstractProcessor {

    private Properties secret;
    private BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
    private Traverser<TimestampedEntry> traverser;

    public TwitterSource() {
        secret = new Properties();
        try {
            secret.load(this.getClass().getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            rethrow(e);
        }
    }

    @Override
    protected void init(Context context) throws Exception {
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        // add some track terms
        List<String> terms = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : CoinDefs.coinMap.entrySet()) {
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
                .build();
        client.connect();
    }

    @Override
    public boolean complete() {
        if (traverser == null) {
            ArrayList<String> messages = new ArrayList<>();
            queue.drainTo(messages);
            List<TimestampedEntry<String, String>> timestampedEntries = messages.stream().map(message -> {
                JSONObject jsonObject = new JSONObject(message);
                if (!jsonObject.has("text")) {
                    return null;
                }
                String text = jsonObject.getString("text");
                return new TimestampedEntry<>(System.currentTimeMillis(), text.toLowerCase(), "");
            }).filter(Objects::nonNull).collect(Collectors.toList());
            traverser = Traversers.traverseIterable(timestampedEntries);
        }
        if (emitFromTraverser(traverser)) {
            traverser = null;
        }
        return false;
    }

}