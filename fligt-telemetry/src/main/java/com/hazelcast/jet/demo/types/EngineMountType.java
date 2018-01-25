package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum EngineMountType {
    NONE(0),
    AFT_MOUNTED(1),
    WING_BURIED(2),
    FUSELAGE_BURIED(3),
    NOSE_MOUNTED(4),
    WING_MOUNTED(5);

    private int id;

    EngineMountType(int id) {
        this.id = id;
    }

    public static EngineMountType fromId(int id) {
        for (EngineMountType type : EngineMountType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
