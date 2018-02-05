package com.betleopard;

/**
 * High-level interface to describe the domain object factories - allows
 * pluggable implementations (including Hazelcast, simple Java 8, Spark)
 * 
 * @author kittylyst
 * @param <T>        the domain type to be built
 */
public interface DomainFactory<T> {
    
    /**
     * The factory implicitly expects lookup by an ID field to be possible
     * 
     * @param ID the ID of the requested object
     * @return   the corresponding object
     */
    public T getByID(long ID);
    
    /**
     * Retrieving an object by name may or may not be possible, this default
     * exists merely for interface convenience.
     * 
     * @param name the name of the instance to be retrieved
     * @return     the corresponding object, or null
     */
    public default T getByName(String name) {
        return null;
    }

    /**
     * Factories may optionally support caching. This method attempts to cache
     * an object of type {@code T}
     * 
     * @param t the object for which caching should be attempted
     * @return  true if the factory supports caching and this object was cached
     */
    public default boolean cacheIfSupported(T t) {
        return false;
    }
    
    /**
     * Factories are expected to manage a long-indexed list of which objects
     * they have created. This method returns the next object ID.
     * 
     * @return the index of the next object to be generated
     */
    public long getNext();
}
