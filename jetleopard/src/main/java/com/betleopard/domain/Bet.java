package com.betleopard.domain;

import com.betleopard.JSONSerializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.betleopard.Builder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code Bet} class models a horse racing bet, which can be either
 * ante post or starting price, and either a simple bet or a traditional
 * accumulator. 
 *
 * It is an immutable type, and uses the Builder pattern to construct
 * new instances of {@code Bet}
 *
 * @author kittylyst
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Bet implements JSONSerializable {
    private final long id;
    private final List<Leg> legs;
    private final double stake;
    private final BetType type;

    @JsonProperty
    public long getID() {
        return id;
    }

    @JsonProperty
    public List<Leg> getLegs() {
        return legs;
    }

    @JsonProperty
    public double getStake() {
        return stake;
    }

    @JsonProperty
    public BetType getType() {
        return type;
    }

    public boolean single() {
        return legs.size() == 1;
    }
    
    private Bet(BetBuilder bb) {
        id = bb.id;
        stake = bb.stake;
        legs = orderLegsByTime(bb.legs);
        if (bb.type == null) {
            type = bb.legs.size() == 1 ? BetType.SINGLE : BetType.ACCUM;
        } else {
            type = bb.type;
        }
    }

    private static List<Leg> orderLegsByTime(final List<Leg> legs) {
        return legs.stream()
                .sorted((l1, l2) -> l1.getRace().raceTime().isBefore(l2.getRace().raceTime()) ? -1 : 1)
                .collect(Collectors.toList());
    }

    /**
     * The {@code Builder} class for {@code Bet} instances
     */
    public static class BetBuilder implements Builder<Bet> {
        private long id;
        private List<Leg> legs = new ArrayList<>();
        private double stake;
        private BetType type;

        @Override
        public Bet build() {
            return new Bet(this);
        }

        public BetBuilder id(final long id) {
            this.id = id;
            return this;
        }

        public BetBuilder stake(final double stake) {
            this.stake = stake;
            return this;
        }

        public BetBuilder type(final BetType type) {
            this.type = type;
            return this;
        }

        public BetBuilder addLeg(final Leg leg) {
            legs.add(leg);
            return this;
        }

        public BetBuilder clearLegs() {
            legs = new ArrayList<>();
            return this;
        }
    }

    /**
     * Provides the projected payout for this bet if a certain horse were to win.
     * Only applies to ante post single bets
     * 
     * @param h the horse under consideration
     * @return  the projected payout from this {@code Bet}
     */
    public double projectedPayout(final Horse h) {
        if (type != BetType.SINGLE || legs.size() > 1)
            throw new IllegalStateException("Projected payout only available for single bets");

        final Leg l = legs.iterator().next();
        if (!l.getBacking().equals(h))
            return 0.0;
        return l.odds() * l.stake();
    }

    /**
     * Calculates the payout for this bet
     *
     * @return the payout from the bet
     */
    public double payout() {
        if (legs.size() == 1) {
            if (type != BetType.SINGLE)
                throw new IllegalStateException("Bet has " + legs.size() + " legs but claims to be a single bet");
            return legs.iterator().next().payout();
        }

        // Accer case
        if (type != BetType.ACCUM)
            throw new IllegalStateException("Bet has " + legs.size() + " legs but claims not to be an accumulator");

        // Check the races have all finished
        for (final Leg l : legs) {
            final Race race = l.getRace();
            if (!race.getWinner().isPresent())
                throw new IllegalArgumentException("Race " + race.toString() + " has not been run");
        }

        // OK, we have run all the races, now order by time 
        final List<Leg> sortedLegs = new ArrayList<>();
        sortedLegs.addAll(legs);
        sortedLegs.sort((a, b) -> a.getRace().raceTime().compareTo(b.getRace().raceTime()));

        double out = stake;
        for (final Leg l : sortedLegs) {
            out = l.payout(out);
        }
        return out;
    }

    /**
     * Factory method for producing a {@code Bet} object from a bag. Used when 
     * deserializing {@code Bet} objects from JSON.
     * 
     * @param bag the bag of parameters
     * @return    the deserialized objects
     */
    public static Bet parseBag(final Map<String, ?> bag) {
        final BetBuilder bb = new BetBuilder();
        bb.id = Long.parseLong("" + bag.get("id"));
        bb.stake = Double.parseDouble("" + bag.get("stake"));
        bb.type = BetType.valueOf("" + bag.get("type"));

        List<Map<String, ?>> legBlobs = (List<Map<String, ?>>) bag.get("legs");
        for (Map<String, ?> lB : legBlobs) {
            bb.addLeg(Leg.parseBag(lB));
        }

        return bb.build();
    }

    @Override
    public String toString() {
        return "Bet{" + "id=" + id + ", legs=" + legs + ", stake=" + stake + ", type=" + type + '}';
    }
}
