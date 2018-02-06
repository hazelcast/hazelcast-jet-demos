package com.betleopard.domain;

import com.betleopard.CustomLegSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * A domain object that represents a single leg of a {@code Bet}. This is one
 * of the more complex domain objects, and uses custom serialization in order
 * to provide usable optional fields.
 * 
 * @author kittylyst
 */
@JsonSerialize(using = CustomLegSerializer.class)
public final class Leg implements Serializable {

    private final Race race;
    private final int oddsVersion;
    private final OddsType oType;
    private transient Optional<Double> odds;
    private final Horse backing;
    private transient Optional<Double> stakeOrAcc;

    public Leg(final long raceID, final long runnerID, final OddsType ot, final Double stake) {
        race = CentralFactory.raceOf(raceID);
        backing = CentralFactory.horseOf(runnerID);
        oType = ot;
        if (oType == OddsType.FIXED_ODDS) {
            odds = Optional.of(race.currentOdds(backing));
            oddsVersion = race.version();
        } else {
            odds = Optional.empty();
            oddsVersion = -1;
        }
        stakeOrAcc = Optional.ofNullable(stake);
    }

    public Leg(final Race r, final Horse runner, final OddsType ot, final Double stake) {
        race = r;
        backing = runner;
        oType = ot;
        if (oType == OddsType.FIXED_ODDS) {
            odds = Optional.of(race.currentOdds(runner));
            oddsVersion = race.version();
        } else {
            odds = Optional.empty();
            oddsVersion = -1;
        }
        stakeOrAcc = Optional.ofNullable(stake);
    }

    public double stake() {
        if (!stakeOrAcc.isPresent()) {
            throw new IllegalStateException("Leg " + toString() + " is not staked yet - part of an accumulator");
        }
        return stakeOrAcc.get();
    }

    public boolean hasStake() {
        return stakeOrAcc.isPresent();
    }

    public double odds() {
        return odds.orElse(race.currentOdds(backing));
    }

    public Race getRace() {
        return race;
    }

    public int getOddsVersion() {
        return oddsVersion;
    }

    public OddsType getoType() {
        return oType;
    }

    public Horse getBacking() {
        return backing;
    }

    /**
     * Calculates the payout of this leg. Fails if the race has not yet been run.
     * 
     * @return the amount to be paid out
     */
    public double payout() {
        final Optional<Horse> winner = race.getWinner();
        if (!winner.isPresent()) {
            throw new IllegalArgumentException("Race " + race.toString() + " has not been run");
        }
        final Horse fptp = winner.get();
        if (backing == fptp) {
            return stake() * odds();
        }
        return 0.0;
    }

    /**
     * Calculates the payout of this leg. Fails if the race has not yet been run.
     * Takes a starting stake so can also be applied to starting price bets
     * 
     * @param startingStake the actual amount staked 
     * @return              the amount to be paid out
     */
    public double payout(final double startingStake) {
        final Optional<Horse> winner = race.getWinner();
        if (!winner.isPresent()) {
            throw new IllegalArgumentException("Race " + race.toString() + " has not been run");
        }
        final Horse fptp = winner.get();
        if (backing == fptp) {
            return startingStake * odds();
        }
        return 0.0;
    }

    @Override
    public String toString() {
        return "Leg{" + "race=" + race + ", oddsVersion=" + oddsVersion + ", oType=" + oType + ", odds=" + odds + ", backing=" + backing + ", stakeOrAcc=" + stakeOrAcc + '}';
    }

    /**
     * Factory method for producing a {@code Leg} object from a bag. Used when 
     * deserializing {@code Leg} objects from JSON.
     * 
     * @param bag the bag of parameters
     * @return    the deserialized objects
     */
    public static Leg parseBag(Map<String, ?> bag) {
        final OddsType type = OddsType.valueOf("" + bag.get("oddsType"));
        final long runnerID = Long.parseLong("" + bag.get("backing"));
        final long raceID = Long.parseLong("" + bag.get("race"));

        Double stake = null;
        final Object st = bag.get("stakes");
        if (st != null) {
            stake = Double.parseDouble("" + st);
        }

        return new Leg(raceID, runnerID, type, stake);
    }

    /**
     * Hook for serialization framework
     * 
     * @param out
     * @throws IOException 
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (odds.isPresent()) {
            out.writeObject(odds.get());
        } else {
            out.writeObject(Double.NaN);
        }
        if (stakeOrAcc.isPresent()) {
            out.writeObject(stakeOrAcc.get());
        } else {
            out.writeObject(Double.NaN);
        }
    }

    /**
     * Hook for serialization framework
     * 
     * @param in
     * @throws IOException 
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final Double oddsVal = (Double) (in.readObject());
        if (oddsVal.equals(Double.NaN)) {
            odds = Optional.empty();
        } else {
            odds = Optional.of(oddsVal);
        }
        final Double stakeVal = (Double) (in.readObject());
        if (stakeVal.equals(Double.NaN)) {
            stakeOrAcc = Optional.empty();
        } else {
            stakeOrAcc = Optional.of(stakeVal);
        }
    }
}
