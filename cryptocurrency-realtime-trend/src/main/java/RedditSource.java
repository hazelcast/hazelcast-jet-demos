import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Submission;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.DefaultPaginator;

import static com.hazelcast.util.ExceptionUtil.rethrow;

public class RedditSource extends AbstractProcessor {

    private Properties secret;
    private Traverser<String> traverser;
    Set<String> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
    RedditClient reddit;

    public RedditSource() {
        secret = new Properties();
        try {
            secret.load(this.getClass().getResourceAsStream("reddit-security.properties"));
        } catch (IOException e) {
            rethrow(e);
        }
    }


    @Override
    protected void init(Context context) throws Exception {
        super.init(context);

        // Create our credentials
        String username = secret.getProperty("username");
        String password = secret.getProperty("password");
        String clientId = secret.getProperty("clientId");
        String clientSecret = secret.getProperty("clientSecret");
        UserAgent userAgent = new UserAgent("bot", "com.hazelcast.jet", "v1.0", username);
        Credentials credentials = Credentials.script(username, password, clientId, clientSecret);

        // This is what really sends HTTP requests
        NetworkAdapter adapter = new OkHttpNetworkAdapter(userAgent);

        // Authenticate and get a RedditClient instance
        reddit = OAuthHelper.automatic(adapter, credentials);
        System.out.println("authenticated to reddit ");

    }

    @Override
    public boolean complete() {
        if (traverser == null) {
            List<String> strings = new ArrayList<>();
            for (String coin : CoinDefs.redditNames) {
                strings.addAll(getSubmissionListForSubreddit(coin));
            }
            strings.stream().map(string -> new TimestampedEntry(System.currentTimeMillis(), string, ""));
            traverser = Traversers.traverseIterable(strings);
        }
        if (emitFromTraverser(traverser)) {
            traverser = null;
        }
        return false;
    }

    public List<String> getSubmissionListForSubreddit(String name) {
        DefaultPaginator<Submission> paginator = getPaginatorForSubreddit(name);
        List<Submission> submissions = paginator.accumulateMerged(-1);
        return analyzeTitleAndContents(submissions);
    }

    public DefaultPaginator<Submission> getPaginatorForSubreddit(String subredditName) {
        return reddit
                .subreddit(subredditName)
                .posts()
                .limit(20)
                .build();

    }

    public List<String> analyzeTitleAndContents(List<Submission> submissions) {
        List<String> texts = new ArrayList<String>();
        for (Submission submission : submissions) {
            if (ids.contains(submission.getId())) {
                continue;
            }
            ids.add(submission.getId());
            texts.add(submission.getTitle() + "." + submission.getSelfText());
        }
        ids.clear();
        return texts;
    }


}