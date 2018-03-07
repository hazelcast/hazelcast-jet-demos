package com.hazelcast.jet.demo.types;

/**
 * The species of the aircraft (helicopter, jet etc.)
 */
public enum Species {
    NONE(0),
    LAND_PLANE(1),
    SEA_PLANE(2),
    AMPHIBIAN(3),
    HELICOPTER(4),
    GYROCOPTER(5),
    TILTWING(6),
    GROUND_VEHICLE(7),
    TOWER(8);

    private int id;

    Species(int id) {
        this.id = id;
    }

    public static Species fromId(int id) {
        for (Species type : Species.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
