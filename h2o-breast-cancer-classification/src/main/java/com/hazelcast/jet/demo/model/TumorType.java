package com.hazelcast.jet.demo.model;

public enum TumorType {

    BENIGN("B"),
    MALIGNENT("M");

    private final String type;

    TumorType(String type) {
        this.type = type;
    }

    public static TumorType fromString(String type) {
        for (TumorType b : TumorType.values()) {
            if (b.type.equalsIgnoreCase(type)) {
                return b;
            }
        }
        return null;

    }

}
