package com.betleopard.simple;

import com.betleopard.DomainFactory;
import com.betleopard.LongIndexed;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple factory class that uses {@code java.util.concurrent} classes to
 * implement the store.
 *
 * @author kittylyst
 * @param <T>        the domain type to be produced
 */
public class SimpleFactory<T extends LongIndexed> implements DomainFactory<T> {

    protected static AtomicLong id = new AtomicLong(1);
    protected final ConcurrentMap<Long, T> cache = new ConcurrentHashMap<>();

    @Override
    public T getByID(long ID) {
        return cache.get(ID);
    }

    @Override
    public long getNext() {
        return id.getAndIncrement();
    }

    @Override
    public boolean cacheIfSupported(T t) {
        cache.put(t.getID(), t);
        return true;
    }

}
