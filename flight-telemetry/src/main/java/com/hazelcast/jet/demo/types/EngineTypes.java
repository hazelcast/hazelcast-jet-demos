package com.hazelcast.jet.demo.types;

/**
 * The type of engine the aircraft uses
 */
public enum EngineTypes {
    NONE(0),
    PISTON(1),
    TURBUPROP(2),
    JET(3),
    ELECTRIC(8);

    private int id;

    EngineTypes(int id) {
        this.id = id;
    }

    public static EngineTypes fromId(int id) {
        for (EngineTypes type : EngineTypes.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
