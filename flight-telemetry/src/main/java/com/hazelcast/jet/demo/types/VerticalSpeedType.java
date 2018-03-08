package com.hazelcast.jet.demo.types;

/**
 * Vertical speed type.
 * 0 = vertical speed is barometric,
 * 1 = vertical speed is geometric.
 * Default to barometric until told otherwise.
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
