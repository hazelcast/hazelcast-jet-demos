package com.hazelcast.jet.demos.market.data.analyzer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MyCLI implements CommandMarker {

    private HazelcastInstance hazelcastInstance;
    private String bootstrapServers;

    public MyCLI(HazelcastInstance hazelcastInstance,
                 @Value("${bootstrap-servers}") String bootstrapServers,
                 @Value("${my.prompt}") String myPrompt) {
        this.hazelcastInstance = hazelcastInstance;
        this.bootstrapServers = bootstrapServers;

        // Initialise all maps
        for (String iMapName : Constants.IMAP_NAMES) {
            log.info("Initialize IMap '{}'", iMapName);
            this.hazelcastInstance.getMap(iMapName);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @CliCommand(value = "LIST", help = "List map keys")
    public void listIMaps() {
        Set<String> iMapNames = this.hazelcastInstance.getDistributedObjects().stream()
                                                      .filter(distributedObject -> distributedObject instanceof IMap)
                                                      .filter(distributedObject -> !distributedObject.getName().startsWith(Jet.INTERNAL_JET_OBJECTS_PREFIX))
                                                      .map(distributedObject -> distributedObject.getName()).collect(Collectors.toCollection(TreeSet::new));

        iMapNames.stream().forEach(name -> {
            IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);

            System.out.println("");
            System.out.printf("IMap: '%s'%n", name);

            // Sort if possible
            Set<?> keys = iMap.keySet();
            if (!keys.isEmpty() && keys.iterator().next() instanceof Comparable) {
                keys = new TreeSet(keys);
            }

            keys.stream().forEach(key -> {
                System.out.printf("    -> '%s' -> %s%n", key, iMap.get(key));
            });

            System.out.printf("[%d entr%s]%n", iMap.size(), (iMap.size() == 1 ? "y" : "ies"));
        });

        System.out.println("");
    }

    @CliCommand(value = "KAFKA", help = "Read from Kafka into the 'precious' IMap")
    public String readKafka() {
        IMap<String, List<String>> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

        List<String> params = new ArrayList<>();
        params.add(Constants.COMMAND_VERB_START);
        params.add(this.bootstrapServers);

        commandMap.put(Constants.COMMAND_NOUN_KAFKA, params);

        return String.format("Requested %s job '%s' with %s", Constants.COMMAND_VERB_START, Constants.COMMAND_NOUN_KAFKA, params.get(1));
    }

    @CliCommand(value = "HISTORY", help = "Recent history of changes to the 'precious' IMap")
    public String readHistory() {
        IMap<String, List<String>> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

        List<String> params = new ArrayList<>();
        params.add(Constants.COMMAND_VERB_START);

        commandMap.put(Constants.COMMAND_NOUN_HISTORY, params);

        return String.format("Requested %s job '%s'", Constants.COMMAND_VERB_START, Constants.COMMAND_NOUN_HISTORY);
    }

}

