package com.hazelcast.jet.demos.market.data.analyzer;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class CommandListener implements EntryAddedListener<String, List<String>>, EntryUpdatedListener<String, List<String>> {

    private static Logger log = LoggerFactory.getLogger(CommandListener.class);

    @Autowired
    private JetInstance jetInstance;

    private Job kafka = null;
    private Job history = null;

    @Override
    public void entryUpdated(EntryEvent<String, List<String>> arg0) {
        try {
            this.handle(arg0);
        } catch (Exception e) {
            log.error("entryUpdated", e);
        }
    }

    @Override
    public void entryAdded(EntryEvent<String, List<String>> arg0) {
        try {
            this.handle(arg0);
        } catch (Exception e) {
            log.error("entryAdded", e);
        }
    }

    private void handle(EntryEvent<String, List<String>> arg0) throws Exception {
        log.info("'{}' '{}'", arg0.getKey(), arg0.getValue());

        String noun = arg0.getKey();
        List<String> params = arg0.getValue();
        String verb = params.get(0);

        if (verb.equalsIgnoreCase(Constants.COMMAND_VERB_START)) {
            this.handleStart(noun, (params.size() == 1 ? null : params.get(1)));
        } else {
            log.error("Unknown command verb '{}'", verb);
        }
    }

    private void handleStart(String noun, String params) {
        if (noun.equalsIgnoreCase(Constants.COMMAND_NOUN_KAFKA)) {
            if (this.kafka == null) {
                Pipeline pipeline = ReadKafkaIntoHazelcast.build(params);
                this.kafka = this.jetInstance.newJob(pipeline);
                log.info("Started Kafka Reader, job id {}", this.kafka.getId());
            } else {
                log.info("Ignoring start request, Kafka Reader job id {} already running", this.kafka.getId());
            }
        } else {
            if (noun.equalsIgnoreCase(Constants.COMMAND_NOUN_HISTORY)) {
                if (this.history == null) {
                    Pipeline pipeline = PreciousHistory.build();
                    this.history = this.jetInstance.newJob(pipeline);
                    log.info("Started History Reader, job id {}", this.history.getId());
                } else {
                    log.info("Ignoring start request, History Reader job id {} already running", this.history.getId());
                }
            } else {
                log.error("Unknown command noun '{}'", noun);
            }
        }
    }

}
