package com.hazelcast.jet.demo.support;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Contains coin to hash-tags lookup table.
 */
public enum CoinType {

    BTC("Bitcoin", "#btc", "#bitcoin"),
    ETH("Ether", "ethereum", "#eth", "#ether", "#ethereum"),
    EOS("EOS", "#eos"),
    XRP("Ripple", "#xrp", "#ripple"),
    BCH("Bitcoin Cash", "#bitcoincash", "#bch"),
    LTC("Litecoin", "#ltc", "#litecoin"),
    XLM("Stellar", "#xlm", "#steller"),
//    ADA("Cardano", "#ada", "#cardano"),
//    XEM("NEM", "#xem", "#nem"),
    ;

    private final String name;
    private final List<String> markers;

    CoinType(String... nameAndMarkers) {
        this.name = nameAndMarkers[0];
        this.markers = asList(nameAndMarkers);
    }

    public String toString() {
        return name;
    }

    public List<String> markers() {
        return markers;
    }
}
