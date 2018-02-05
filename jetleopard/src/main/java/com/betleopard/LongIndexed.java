package com.betleopard;

/**
 * A simple interface for domain objects, which are expected to be indexed by
 * a long value that represents the ID of the object.
 *
 * @author kittylyst
 */
public interface LongIndexed {
    /**
     * This method returns the ID of the current object
     * 
     * @return the long index ID of this object
     */
    long getID();
}
