package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum AltitudeType {
    STANDARD_PRESSURE(0),
    INDICATED(1);

    private int id;

    AltitudeType(int id) {
        this.id = id;
    }

    public static AltitudeType fromId(int id) {
        for (AltitudeType type : AltitudeType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
