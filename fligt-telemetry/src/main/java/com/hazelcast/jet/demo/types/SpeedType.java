package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum SpeedType {
    GROUND_SPEED(0),
    GROUND_SPEED_REVERSING(1),
    INDICATED_AIR_SPEED(2),
    TRUE_AIR_SPEED(3);

    private int id;

    SpeedType(int id) {
        this.id = id;
    }

    public static SpeedType fromId(int id) {
        for (SpeedType type : SpeedType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
