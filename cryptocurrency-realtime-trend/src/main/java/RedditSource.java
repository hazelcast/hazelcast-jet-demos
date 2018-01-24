import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.ProcessorSupplier;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Submission;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.DefaultPaginator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedditSource implements ProcessorSupplier {

    private Properties secret;

    public RedditSource() {
        try {
            secret = new Properties();
            secret.load(this.getClass().getResourceAsStream("reddit-security.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Collection<? extends Processor> get(int count) {
        AbstractProcessor abstractProcessor = new AbstractProcessor() {
            Set<String> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
            RedditClient reddit;
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
                    if (ids.contains(submission.getId())) continue;
                    ids.add(submission.getId());
                    texts.add(submission.getTitle() + "." + submission.getSelfText());
                }
                ids.clear();
                return texts;
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
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(String coin:CoinDefs.redditNames){
                    List<String> strings = getSubmissionListForSubreddit(coin);
                    for (String string : strings) {
                        if (!tryEmit(string)) {
                            return false;
                        }
                    }
                }
                return false;

            }

        };
        return Collections.singleton(abstractProcessor);

    }

}
