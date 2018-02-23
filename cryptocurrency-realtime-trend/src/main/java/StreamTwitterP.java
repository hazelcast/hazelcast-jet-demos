import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.CloseableProcessorSupplier;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nonnull;
import org.json.JSONObject;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;

public class StreamTwitterP extends AbstractProcessor implements Closeable {

    private final Properties properties;
    private final List<String> terms;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);
    private final ArrayList<String> buffer = new ArrayList<>();

    private Traverser<String> traverser;
    private BasicClient client;

    private StreamTwitterP(Properties properties, List<String> terms) {
        this.properties = properties;
        this.terms = terms;
    }

    @Override
    protected void init(@Nonnull Context context) {
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        endpoint.trackTerms(terms);

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

    @Override
    public boolean complete() {
        if (traverser == null) {
            if (queue.drainTo(buffer) == 0) {
                return false;
            } else {
                traverser = traverseIterable(buffer)
                        .map(JSONObject::new)
                        .filter(json -> json.has("text"))
                        .map(json -> json.getString("text"));
            }
        }
        if (emitFromTraverser(traverser)) {
            buffer.clear();
            traverser = null;
        }
        return false;
    }

    private boolean isMissing(String test) {
        return test.isEmpty() || "REPLACE_THIS".equals(test);
    }

    @Override
    public boolean isCooperative() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            client.stop();
        }
    }

    public static ProcessorMetaSupplier streamTwitterP(Properties properties, List<String> terms) {
        return dontParallelize(new CloseableProcessorSupplier<>(() -> new StreamTwitterP(properties, terms)));
    }

    public static StreamSource<String> streamTwitter(Properties properties, List<String> terms) {
        return Sources.streamFromProcessor("twitterSource", streamTwitterP(properties, terms));
    }

}