package com.jetleopard;

import com.betleopard.JSONSerializable;
import com.betleopard.domain.CentralFactory;
import com.betleopard.domain.Event;
import com.betleopard.domain.Horse;
import com.betleopard.simple.SimpleFactory;
import com.betleopard.simple.SimpleHorseFactory;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.DistributedFunctions.wholeItem;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple example for getting started - uses Jet adapted from Java 8
 * collections
 *
 * @author kittylyst
 */
public class AnalysisJet {

    public final static String EVENTS_BY_NAME = "events_by_name";
    public final static String MULTIPLE = "multiple_winners";
    public final static String HISTORICAL = "/historical_races.json";

    public final static Function<Event, Horse> FIRST_PAST_THE_POST = e -> e.getRaces().get(0).getWinner().orElse(Horse.PALE);

    public final static DistributedFunction<Entry<String, Event>, Horse> HORSE_FROM_EVENT = e -> FIRST_PAST_THE_POST.apply(e.getValue());

    private JetInstance jet;

    public static void main(String[] args) throws Exception {
        CentralFactory.setHorses(SimpleHorseFactory.getInstance());
        CentralFactory.setRaces(new SimpleFactory<>());
        final AnalysisJet main = new AnalysisJet();

        main.setup();
        try {
            main.go();
            final Map<Horse, Long> multiple = main.jet.getMap(MULTIPLE);
            System.out.println("Result set size: " + multiple.size());
            for (Horse h : multiple.keySet()) {
                System.out.println(h + " : " + multiple.get(h));
            }
        } finally {
            Jet.shutdownAll();
        }
    }

    /**
     * Populate a map with data from disc
     */
    public void setup() {
        jet = Jet.newJetInstance();

        final IMap<String, Event> name2Event = jet.getMap(EVENTS_BY_NAME);

        try (BufferedReader r = new BufferedReader(new InputStreamReader(AnalysisJet.class.getResourceAsStream(HISTORICAL), UTF_8))) {
            r.lines().map(l -> JSONSerializable.parse(l, Event::parseBag))
             .forEach(e -> name2Event.put(e.getName(), e));
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    public void go() {
        System.out.print("\nStarting up... ");
        long start = System.nanoTime();

        Pipeline p = buildPipeline();
        jet.newJob(p).join();

        System.out.println("done in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " milliseconds.");
    }

    /**
     * Helper method to construct the pipeline for the job
     *
     * @return the pipeline for the job
     */
    public static Pipeline buildPipeline() {
        final Pipeline p = Pipeline.create();

        // Compute map server side
        final BatchStage<Horse> c = p.drawFrom(Sources.map(EVENTS_BY_NAME, t -> true, HORSE_FROM_EVENT));

        final BatchStage<Entry<Horse, Long>> c2 = c.groupingKey(wholeItem())
                                                   .aggregate(counting())
                                                   .filter(ent -> ent.getValue() > 1);

        c2.drainTo(Sinks.map(MULTIPLE));

        return p;
    }

}
