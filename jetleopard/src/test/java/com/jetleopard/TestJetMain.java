package com.jetleopard;

import com.betleopard.JSONSerializable;
import com.betleopard.domain.*;
import com.betleopard.domain.Bet.BetBuilder;
import com.betleopard.hazelcast.HazelcastFactory;
import com.betleopard.hazelcast.HazelcastHorseFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.stream.IStreamMap;
import static com.jetleopard.JetBetMain.WORST_ID;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static java.time.temporal.TemporalAdjusters.next;
import java.util.Map;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ben
 */
public class TestJetMain {

    private HazelcastInstance client;
    private JetInstance jet;

    @Before
    public void setup() throws Exception {
        jet = Jet.newJetInstance();
        client = jet.getHazelcastInstance();

        CentralFactory.setHorses(HazelcastHorseFactory.getInstance(client));
        CentralFactory.setRaces(new HazelcastFactory<>(client, Race.class));
        CentralFactory.setEvents(new HazelcastFactory<>(client, Event.class));
        CentralFactory.setUsers(new HazelcastFactory<>(client, User.class));
        CentralFactory.setBets(new HazelcastFactory<>(client, Bet.class));
    }

    @Test
    public void testBuildDAG() throws Exception {
        User u = makeUser();
        Bet b = makeBet();
        u.addBet(b);
        assertNotNull(b);
        try {
            IStreamMap<String, ?> ism = jet.getMap(WORST_ID);
            System.out.println(ism);
            System.out.println("Size: " + ism.size());
            for (String s : ism.keySet()) {
                System.out.println(s + " : " + ism.get(s));
            }
        } finally {
            Jet.shutdownAll();
        }
    }

    private User makeUser() {
        return new User(1L, "Ben", "Evans");
    }

    private Bet makeBet() {
        String raw = "{\"id\":1,\"name\":\"CHELTENHAM 1924-03-14\",\"date\":{\"year\":1924,\"month\":\"MARCH\",\"dayOfMonth\":14,\"dayOfWeek\":\"FRIDAY\",\"era\":\"CE\",\"dayOfYear\":74,\"leapYear\":true,\"monthValue\":3,\"chronology\":{\"id\":\"ISO\",\"calendarType\":\"iso8601\"}},\"races\":[{\"id\":1,\"hasRun\":true,\"currentVersion\":{\"odds\":{\"1\":6.0,\"2\":3.0},\"raceTime\":{\"nano\":0,\"hour\":12,\"minute\":30,\"second\":0,\"year\":1924,\"month\":\"MARCH\",\"dayOfMonth\":14,\"dayOfWeek\":\"FRIDAY\",\"dayOfYear\":74,\"monthValue\":3,\"chronology\":{\"id\":\"ISO\",\"calendarType\":\"iso8601\"}},\"version\":0,\"runners\":[{\"name\":\"Red Splash\",\"id\":1},{\"name\":\"Not Red Splash\",\"id\":2}]},\"winner\":{\"name\":\"Red Splash\",\"id\":1}}]}";

        Event cheltenam04 = JSONSerializable.parse(raw, Event::parseBag);
        Map<Horse, Double> odds = cheltenam04.getRaces().get(0).currentOdds();

        // Now set up a new event based on the dummy one
        final LocalDate nextSat = LocalDate.now().with(next(DayOfWeek.SATURDAY));
        final LocalTime raceTime = LocalTime.of(11, 0); // 1100 start

        Event e = CentralFactory.eventOf("Test Next Saturday", nextSat);
        Race r = CentralFactory.raceOf(LocalDateTime.of(nextSat, raceTime), odds);
        e.addRace(r);

        BetBuilder bb = CentralFactory.betOf();
        Leg l = new Leg(r, r.findRunnerByID(0), OddsType.FIXED_ODDS, 2.0);

        return bb.stake(2).addLeg(l).build();
    }

}
