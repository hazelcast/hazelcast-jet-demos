package com.hazelcast.jet.demo.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains coin to hash-tags lookup table.
 */
public class CoinDefs {

    public static Map<String, List<String>> COIN_MAP = new HashMap<>();

    public static final int SYMBOL = 0;

    static {
        COIN_MAP.put("BTC", Arrays.asList("Bitcoin", "#btc", "#bitcoin"));
        COIN_MAP.put("ETH", Arrays.asList("Ether", "ethereum", "#eth", "#ether", "#ethereum"));
        COIN_MAP.put("XRP", Arrays.asList("Ripple", "#xrp", "#ripple"));
        COIN_MAP.put("BCH", Arrays.asList("Bitcoin Cash", "#bitcoincash", "#bch"));
        COIN_MAP.put("ADA", Arrays.asList("Cardano", "#ada", "#cardano"));
        COIN_MAP.put("LTC", Arrays.asList("Litecoin", "#ltc", "#litecoin"));
        COIN_MAP.put("XLM", Arrays.asList("Stellar", "#xlm", "#steller"));
        COIN_MAP.put("XEM", Arrays.asList("NEM", "#xem", "#nem"));
        COIN_MAP.put("EOS", Arrays.asList("EOS", "#eos"));
    }

}
