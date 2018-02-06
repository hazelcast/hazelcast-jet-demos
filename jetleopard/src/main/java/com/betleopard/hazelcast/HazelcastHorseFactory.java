package com.betleopard.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.betleopard.domain.Horse;
import java.util.Collection;
import java.util.Optional;

/**
 * A concrete {@code Horse} factory that demonstrates extending the factory
 * with the optional "lookup by name" functionality
 * 
 * @author kittylyst
 */
public final class HazelcastHorseFactory extends HazelcastFactory<Horse> {

    private static HazelcastHorseFactory instance;

    private HazelcastHorseFactory(final HazelcastInstance instance) {
        super(instance, Horse.class);
    }

    public static synchronized HazelcastHorseFactory getInstance(HazelcastInstance client) {
        if (instance == null) {
            instance = new HazelcastHorseFactory(client);
        }
        return instance;
    }
    
    @Override
    public synchronized Horse getByName(final String name) {
        final Collection<Horse> stud = cache.values();
                
        final Optional<Horse> horse = stud.stream()
                                .filter(h -> h.getName().equals(name))
                                .findFirst();
        
        if (horse.isPresent()) 
            return horse.get();

        final Horse newHorse = new Horse(name, id.getAndIncrement());
        cache.put(newHorse.getID(), newHorse);
        return newHorse;
    }
}
