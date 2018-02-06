package com.betleopard.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.betleopard.DomainFactory;
import com.betleopard.LongIndexed;

/**
 * A simple base class that backs the domain factories with Hazelcast IMDG,
 * using the simple API of IMap and IAtomicLong
 *
 * @author kittylyst
 * @param <T>
 */
public class HazelcastFactory<T extends LongIndexed> implements DomainFactory<T> {

    private final HazelcastInstance hz;

    // Move to a Hazelcast factory using IAtomicLong
    protected IAtomicLong id;
    protected IMap<Long, T> cache;
    protected String className;

    public HazelcastFactory(final HazelcastInstance instance, final Class<T> classOfT) {
        hz = instance;
        cache = hz.getMap("cache-" + classOfT);
        id = hz.getAtomicLong("counter-" + classOfT);
    }

    @Override
    public T getByID(long ID) {
        return cache.get(ID);
    }

    @Override
    public boolean cacheIfSupported(T t) {
        cache.put(t.getID(), t);
        return true;
    }

    @Override
    public long getNext() {
        return id.getAndIncrement();
    }

}
