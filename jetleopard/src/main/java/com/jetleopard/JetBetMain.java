package com.jetleopard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.betleopard.JSONSerializable;
import com.betleopard.domain.*;
import com.betleopard.Utils;
import com.betleopard.hazelcast.HazelcastFactory;
import com.betleopard.hazelcast.HazelcastHorseFactory;
import com.betleopard.hazelcast.RandomSimulator;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import com.hazelcast.jet.*;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import static com.hazelcast.jet.Traversers.traverseStream;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JetConfig;
import static com.hazelcast.jet.datamodel.Tuple2.tuple2;
import static com.hazelcast.jet.datamodel.Tuple3.tuple3;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;

/**
 * The main example driver class. Uses both Hazelcast Jet backed by IMDG to perform
 * data storage and live analysis of in-running bets.
 *
 * @author kittylyst
 */
public final class JetBetMain implements RandomSimulator {

    private JetInstance jet;
    private Pipeline pipeline;

    private Path filePath;

    private volatile boolean shutdown = false;

    public final static String USER_ID = "users";
    public final static String EVENT_ID = "events";
    public static final String WORST_ID = "worst_case";

    public static void main(String[] args) throws Exception {
        final JetBetMain main = new JetBetMain();
        main.init();
        main.run();
        main.stop();
    }

    @Override
    public HazelcastInstance getClient() {
        return jet.getHazelcastInstance();
    }

    private void init() throws IOException, URISyntaxException {
        final JetConfig config = new JetConfig();
        jet = Jet.newJetInstance(config);
        pipeline = buildPipeline();

        // Initialize the domain object factories
        final HazelcastInstance client = jet.getHazelcastInstance();
        CentralFactory.setHorses(HazelcastHorseFactory.getInstance(client));
        CentralFactory.setRaces(new HazelcastFactory<>(client, Race.class));
        CentralFactory.setEvents(new HazelcastFactory<>(client, Event.class));
        CentralFactory.setUsers(new HazelcastFactory<>(client, User.class));
        CentralFactory.setBets(new HazelcastFactory<>(client, Bet.class));

        loadHistoricalRaces();
        createRandomUsers();
        createFutureEvent();
    }

    public void stop() throws IOException {
        Jet.shutdownAll();
        Utils.cleanupDataInTmp(filePath);
    }

    /**
     * Main run loop 
     */
    public void run() {
        while (!shutdown) {
            addSomeSimulatedBets();
            final Job job = jet.newJob(pipeline);
            job.join();
            outputPossibleLosses();
            try {
                // Simulated delay
                Thread.sleep(20_000);
            } catch (InterruptedException ex) {
                shutdown = true;
            }
        }
        jet.shutdown();
    }

    /**
     * Helper method to construct the pipeline for the job
     * 
     * @return the pipeline for the real-time analysis
     */
    public static Pipeline buildPipeline() {
        final Pipeline pipeline = Pipeline.create();

        // Draw users from the Hazelcast IMDG source
        ComputeStage<User> users = pipeline.drawFrom(Sources.<Long, User, User>map(USER_ID, e -> true, Entry::getValue));

        // All bet legs which are single
        ComputeStage<Tuple3<Race, Horse, Bet>> bets = users.flatMap(user -> traverseStream(
                user.getKnownBets().stream()
                    .filter(Bet::single)
                    .flatMap(bet -> bet.getLegs().stream().map(leg -> tuple3(leg.getRace(), leg.getBacking(), bet)))
            )
        );

        // Find for each race the projected loss if each horse was to win
        ComputeStage<Entry<Race, Map<Horse, Double>>> betsByRace = bets.groupBy(
                Tuple3::f0, AggregateOperations.toMap(
                        Tuple3::f1,
                        t -> t.f2().projectedPayout(t.f1()), // payout if backed horse was to win
                        (l, r) -> l + r
                )
        );

        // Write out: (r : (h : losses))
        betsByRace.drainTo(Sinks.map(WORST_ID));

        return pipeline;
    }

    /**
     * Pick the largest exposure from a input map.
     * 
     * @param exposures exposed amount per horse
     * @return the maximal horse-amount pair
     */
    public static final Tuple2<Horse, Double> getMaxExposureAsTuple(Map<Horse, Double> exposures) {
        return exposures.entrySet().stream()
                .max(Entry.comparingByValue())
                .map(e -> tuple2(e.getKey(), e.getValue()))
                .get();
    }

    /**
     * Helper method that calculates largest possible loss and the results that
     * caused that outcome.
     */
    public void outputPossibleLosses() {
        final IMap<Race, Map<Horse, Double>> risks = jet.getHazelcastInstance().getMap(WORST_ID);

        final Double apocalypse = risks.entrySet().stream()
                .map(e -> tuple2(e.getKey(), getMaxExposureAsTuple(e.getValue())))
                .sorted((t1, t2) -> t1.f1().f1().compareTo(t2.f1().f1()))
                .limit(20)
                
                // Output "perfect storm" combination of top 20 results that caused the losses
                .peek(t -> System.out.println("Horse: " + t.f1().f0().getName() + " ; Losses: " + t.f1().f1()))
                
                // Finally output the maximum possible loss
                .map(tr -> tr.f1())
                .map(Entry<Horse, Double>::getValue)
                .reduce(0.0, (ra, rb) -> ra + rb);

        System.out.println("Worst case total losses: " + apocalypse);
    }

    /**
     * Loads in historical data and stores in Hazelcast IMDG. This is mostly to 
     * provide a source of horses for the bet simulation.
     * 
     * @throws IOException some form of IO problem was encountered when opening the historical data
     */
    public void loadHistoricalRaces() throws IOException {
        filePath = Utils.unpackDataToTmp("historical_races.json");

        final List<String> eventsText = Files.readAllLines(filePath);

        final List<Event> events
                = eventsText.stream()
                .map(s -> JSONSerializable.parse(s, Event::parseBag))
                .collect(Collectors.toList());

        final HazelcastInstance client = jet.getHazelcastInstance();

        final Function<Event, Horse> fptp = raceEvent -> raceEvent.getRaces().get(0).getWinner().orElse(Horse.PALE);
        final Map<Event, Horse> winners
                = events.stream()
                .collect(Collectors.toMap(identity(), fptp));

        final Map<Horse, List<Event>> inverted = new HashMap<>();
        for (Map.Entry<Event, Horse> entry : winners.entrySet()) {
            if (inverted.get(entry.getValue()) == null) {
                inverted.put(entry.getValue(), new ArrayList<>());
            }
            inverted.get(entry.getValue()).add(entry.getKey());
        }
        final IMap<Horse, List<Event>> invertHC = client.getMap("inverted");
        for (final Horse horse : inverted.keySet()) {
            invertHC.put(horse, inverted.get(horse));
        }

        final Function<Map.Entry<Horse, ?>, Horse> under1 = entry -> entry.getKey();
        final Function<Map.Entry<Horse, Integer>, Integer> under2 = entry -> entry.getValue();
        final Function<Map.Entry<Horse, List<Event>>, Integer> setCount = entry -> entry.getValue().size();
        final Map<Horse, Integer> withWinCount
                = inverted.entrySet().stream()
                .collect(Collectors.toMap(under1, setCount));

        final Map<Horse, Integer> multipleWinners
                = withWinCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(under1, under2));

        final IMap<Horse, Integer> fromHC = client.getMap("winners");
        for (final Horse horse : multipleWinners.keySet()) {
            System.out.println("Putting: " + horse + " : " + multipleWinners.get(horse));
            fromHC.put(horse, multipleWinners.get(horse));
        }
    }
}
