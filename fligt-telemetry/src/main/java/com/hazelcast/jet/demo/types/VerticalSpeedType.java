package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum VerticalSpeedType {
    BAROMETRIC(0),
    GEOMETRIC(1);

    private int id;

    VerticalSpeedType(int id) {
        this.id = id;
    }

    public static VerticalSpeedType fromId(int id) {
        for (VerticalSpeedType type : VerticalSpeedType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
