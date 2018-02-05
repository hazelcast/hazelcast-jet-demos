package com.betleopard.hazelcast;

import com.betleopard.domain.Leg;
import com.betleopard.domain.Horse;
import com.betleopard.domain.Event;
import com.betleopard.domain.Race;
import com.betleopard.domain.Bet;
import com.betleopard.domain.OddsType;
import com.betleopard.domain.CentralFactory;
import com.betleopard.domain.User;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toSet;

/**
 * Base trait for the main live analysis class. Contains boilerplate methods
 * for random simulations.
 *
 * @author kittylyst
 */
public interface RandomSimulator {

    public static final int NUM_USERS = 100;

    public HazelcastInstance getClient();
    /**
     * Set up an event to hang the bets off  
     */
    public default void createFutureEvent() {
        // Grab some horses to use as runners in races
        final IMap<Horse, Object> fromHC = getClient().getMap("winners");
        final Set<Horse> horses = fromHC.keySet();

        // Now set up some future-dated events for next Sat
        final LocalDate nextSat = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalTime raceTime = LocalTime.of(11, 0); // 1100 start
        final Event e = CentralFactory.eventOf("Racing from Epsom", nextSat);
        final Set<Horse> runners = makeRunners(horses, 10);
        for (int i = 0; i < 18; i++) {
            final Map<Horse, Double> runnersWithOdds = makeSimulatedOdds(runners);
            final Race r = CentralFactory.raceOf(LocalDateTime.of(nextSat, raceTime), runnersWithOdds);
            e.addRace(r);

            raceTime = raceTime.plusMinutes(10);
        }
        final IMap<Long, Event> events = getClient().getMap("events");
        events.put(e.getID(), e);
    }

    /**
     * Generates some simulated bets to test the risk reporting
     */
    public default void addSomeSimulatedBets() {
        final IMap<Long, Event> events = getClient().getMap("events");
        final IMap<Long, User> users = getClient().getMap("users");
        System.out.println("Events: " + events.size());
        System.out.println("Users: " + users.size());

        final int numBets = 100;
        for (int i = 0; i < numBets; i++) {
            final Race r = getRandomRace(events);
            final Map<Long, Double> odds = r.getCurrentVersion().getOdds();
            final Horse shergar = getRandomHorse(r);
            final Leg l = new Leg(r, shergar, OddsType.FIXED_ODDS, 2.0);
            final Bet.BetBuilder bb = CentralFactory.betOf();
            final Bet b = bb.addLeg(l).stake(l.stake()).build();
            final int rU = new Random().nextInt(users.size());
            User u = null;
            int j = 0;
            USERS:
            for (final User tmp : users.values()) {
                if (j >= rU) {
                    u = tmp;
                    break USERS;
                }
                j++;
            }
            if (u == null)
                throw new IllegalStateException("Failed to pick a user for a random bet");
            if (!u.addBet(b)) {
                System.out.println("Bet " + b + " not added successfully");
            }
            users.put(u.getID(), u);
        }
        int betCount = 0;
        for (final User u : users.values()) {
            betCount += u.getKnownBets().size();
        }
        System.out.println("Total Bets: " + betCount);
    }

    /**
     * Utility method to get some horses for simulated races
     * 
     * @param horses
     * @param num
     * @return 
     */
    public static Set<Horse> makeRunners(final Set<Horse> horses, int num) {
        return horses.stream().limit(num).collect(toSet());
    }

    /**
     * Create some simulated odds for this set of runners
     * 
     * @param runners
     * @return 
     */
    public static Map<Horse, Double> makeSimulatedOdds(final Set<Horse> runners) {
        final AtomicInteger count = new AtomicInteger(1);
        return runners.stream()
                .limit(4)
                .collect(Collectors.toMap(identity(), h -> Math.random() * count.getAndIncrement()));

    }

    /**
     * Return a {@code Race} at random from the provided set
     * 
     * @param eventsByID
     * @return 
     */
    public static Race getRandomRace(final IMap<Long, Event> eventsByID) {
        final List<Event> events = new ArrayList<>(eventsByID.values());
        final int rI = new Random().nextInt(events.size());
        final Event theDay = events.get(rI);
        final List<Race> races = theDay.getRaces();
        final int rR = new Random().nextInt(races.size());
        return races.get(rR);
    }

    /**
     * Return a random horse from the set of runners in the provided {@code Race}
     * 
     * @param r
     * @return 
     */
    public static Horse getRandomHorse(final Race r) {
        final List<Horse> geegees = new ArrayList<>(r.getCurrentVersion().getRunners());
        final int rH = new Random().nextInt(geegees.size());
        return geegees.get(rH);
    }

    /**
     * Sets up some random users (to place bets) and stores them in Hazlecast IMDG
     */
    public default void createRandomUsers() {
        final IMap<Long, User> users = getClient().getMap("users");

        final String[] firstNames = {"Dave", "Christine", "Sarah", "Sadiq", "Zoe", "Helen", "Mike", "George", "Joanne"};
        final String[] lastNames = {"Baker", "Jones", "Smith", "Singh", "Shah", "Johnson", "Taylor", "Evans", "Howe"};
        final Random r = new Random();
        for (int i = 0; i < NUM_USERS; i++) {
            final User u = CentralFactory.userOf(firstNames[r.nextInt(firstNames.length)], lastNames[r.nextInt(lastNames.length)]);
            users.put(u.getID(), u);
        }
    }

}
