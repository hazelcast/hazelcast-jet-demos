package com.hazelcast.jet.demos.market.data.ingest;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:application.yml")
public class ApplicationConfig {

    @Bean
    public Config config(Environment environment) {
        return new ClasspathXmlConfig("hazelcast.xml");
    }

    @Bean
    public JetInstance jetInstance(Config config) {
        JetConfig jetConfig = new JetConfig().setHazelcastConfig(config);
        return Jet.newJetInstance(jetConfig);
    }

    @Bean
    public HazelcastInstance hazelcastInstance(CommandListener commandListener, JetInstance jetInstance) {
        HazelcastInstance hazelcastInstance = jetInstance.getHazelcastInstance();

        // React to map changes
        IMap<?, ?> commandMap = hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);
        commandMap.addLocalEntryListener(commandListener);

        // Add in, if we want to trace map changes
        IMap<?, ?> preciousMap =
                hazelcastInstance.getMap(Constants.IMAP_NAME_PRECIOUS);
        preciousMap.addLocalEntryListener(new LoggingListener());

        return hazelcastInstance;
    }
}
