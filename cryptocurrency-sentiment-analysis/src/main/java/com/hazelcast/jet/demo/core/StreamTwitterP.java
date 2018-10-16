/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.demo.core;

import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.preferLocalParallelismOne;
import static com.hazelcast.jet.demo.util.Util.isMissing;

/**
 * A streaming source that connects and pull the tweets from Twitter using official Twitter client.
 */
public class StreamTwitterP extends AbstractProcessor {

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
        return preferLocalParallelismOne(ProcessorSupplier.of(() -> new StreamTwitterP(properties, terms)));
    }

}
