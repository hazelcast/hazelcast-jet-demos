package com.hazelcast.jet.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class Constants {

    static final SortedMap<Integer, Integer> mediumWTCDescendAltitudeToNoiseDb = new TreeMap<Integer, Integer>() {{
        put(3000, 63);
        put(2000, 71);
        put(1000, 86);
        put(500, 93);
    }};
    static final SortedMap<Integer, Integer> heavyWTCDescendAltitudeToNoiseDb = new TreeMap<Integer, Integer>() {{
        put(3000, 70);
        put(2000, 79);
        put(1000, 94);
        put(500, 100);
    }};
    static final SortedMap<Integer, Integer> heavyWTCClimbingAltitudeToNoiseDb = new TreeMap<Integer, Integer>() {{
        put(500, 96);
        put(1000, 92);
        put(1500, 86);
        put(2000, 82);
        put(3000, 72);
    }};

    static final SortedMap<Integer, Integer> mediumWTCClimbingAltitudeToNoiseDb = new TreeMap<Integer, Integer>() {{
        put(500, 83);
        put(1000, 81);
        put(1500, 74);
        put(2000, 68);
        put(3000, 61);
    }};


    static final Map<String, Double> typeToLTOCycyleC02Emission = new HashMap<String, Double>() {{
        put("B738", 2625d);
        put("A320", 2750.7d);
        put("A321", 2560d);
        put("A319", 2169.8d);
        put("B763", 5405d);
        put("E190", 1605d);
        put("B773", 7588d);
        put("A333", 5934d);
        put("B752", 4292d);
        put("A332", 7029d);
        put("B737", 2454d);
        put("DH8D", 1950d);
        put("B772", 7346d);
        put("B788", 10944d);
        put("B739", 2500d);
        put("A388", 13048d);
        put("B744", 10456d);
        put("B789", 10300d);
        put("E170", 1516d);
        put("A306", 5200d);
        put("B77L", 9076d);
        put("MD11", 8277d);
        put("E145", 1505d);
        put("B77W", 9736d);
        put("A318", 2169d);
        put("B748", 10400d);
        put("B735", 2305d);
        put("A359", 6026d);
        put("CRJ9", 2754d);
        put("CRJ2", 1560d);
        put("CRJ7", 2010d);
        put("DC10", 7460d);
    }};

}
