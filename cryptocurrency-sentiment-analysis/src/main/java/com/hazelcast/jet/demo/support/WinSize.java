package com.hazelcast.jet.demo.support;

public enum WinSize {
    HALF_MINUTE(30_000L, "Last 30 seconds"), FIVE_MINUTES(300_000L, "Last 5 minutes");

    private long duration;
    private String label;

    WinSize(long duration, String label) {
        this.duration = duration;
        this.label = label;
    }

    public long durationMillis() {
        return duration;
    }

    @Override
    public String toString() {
        return label;
    }
}
