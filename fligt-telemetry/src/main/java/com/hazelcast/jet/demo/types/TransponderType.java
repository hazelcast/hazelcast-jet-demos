package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum TransponderType {
    UNKNOWN(0),
    MODE_S(1),
    ADS_B_UNKNOWN_VERSION(2),
    ADS_B_0(3),
    ADS_B_1(4),
    ADS_B_2(5);

    private int id;

    TransponderType(int id) {
        this.id = id;
    }

    public static TransponderType fromId(int id) {
        for (TransponderType type : TransponderType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
