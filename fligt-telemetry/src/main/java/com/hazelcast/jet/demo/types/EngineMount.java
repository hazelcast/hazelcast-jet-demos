package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum EngineMount {
    NONE(0),
    AFT_MOUNTED(1),
    WING_BURIED(2),
    FUSELAGE_BURIED(3),
    NOSE_MOUNTED(4),
    WING_MOUNTED(5);

    private int id;

    EngineMount(int id) {
        this.id = id;
    }

    public static EngineMount fromId(int id) {
        for (EngineMount type : EngineMount.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
