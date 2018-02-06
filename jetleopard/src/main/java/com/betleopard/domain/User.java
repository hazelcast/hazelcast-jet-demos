package com.betleopard.domain;

import com.betleopard.JSONSerializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple domain object that describes a user, ie a person placing a bet.
 * Like other domain objects, it is immutable and {@code JSONSerializable}
 *
 * @author kittylyst
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements JSONSerializable {
    private final long id;
    private final String firstName;
    private final String lastName;
    private final List<Bet> knownBets = new LinkedList<>();

    public User(final long ID, final String first, final String last) {
        id = ID;
        firstName = first;
        lastName = last;
    }

    @JsonProperty
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty
    public String getLastName() {
        return lastName;
    }

    @JsonProperty
    public long getID() {
        return id;
    }
    
    @JsonProperty
    public List<Bet> getKnownBets() {
        return knownBets;
    }

    public boolean addBet(final Bet b) {
        // FIXME Enforce ordering here...?
        return knownBets.add(b);
    }

    public boolean removeBet(final Bet b) {
        return knownBets.remove(b);
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", knownBets=" + knownBets + '}';
    }

}
