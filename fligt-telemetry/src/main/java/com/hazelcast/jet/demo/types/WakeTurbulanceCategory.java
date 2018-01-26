package com.hazelcast.jet.demo.types;

/**
 * date: 1/8/18
 * author: emindemirci
 */
public enum WakeTurbulanceCategory {
    NONE(0),
    LIGHT(1),
    MEDIUM(2),
    HEAVY(3);

    private int id;

    WakeTurbulanceCategory(int id) {
        this.id = id;
    }

    public static WakeTurbulanceCategory fromId(int id) {
        for (WakeTurbulanceCategory type : WakeTurbulanceCategory.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return NONE;
    }

}
