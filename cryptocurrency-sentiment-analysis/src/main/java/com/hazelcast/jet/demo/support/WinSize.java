package com.hazelcast.jet.demo.support;

public enum WinSize {
    HALF_MINUTE("Last 30 seconds"), FIVE_MINUTES("Last 5 minutes");

    private String label;

    WinSize(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
