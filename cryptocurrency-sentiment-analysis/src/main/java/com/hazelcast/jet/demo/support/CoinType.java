package com.hazelcast.jet.demo.support;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Contains coin to hash-tags lookup table.
 */
public enum CoinType {

    BTC("Bitcoin",      asList("Bitcoin", "#btc", "#bitcoin")),
    ETH("Ethereum",     asList("Ether", "ethereum", "#eth", "#ether", "#ethereum")),
    XRP("Ripple",       asList("Ripple", "#xrp", "#ripple")),
    BCH("Bitcoin Cash", asList("Bitcoin Cash", "#bitcoincash", "#bch")),
    LTC("Litecoin",     asList("Litecoin", "#ltc", "#litecoin")),
    XLM("Stellar",      asList("Stellar", "#xlm", "#steller")),
    EOS("EOS",          asList("EOS", "#eos"))
//    ADA("Cardano",      asList("Cardano", "#ada", "#cardano")),
//    XEM("NEM",          asList("NEM", "#xem", "#nem")),
    ;

    private final String name;
    private final List<String> markers;

    CoinType(String name, List<String> markers) {
        this.name = name;
        this.markers = markers;
    }

    public String toString() {
        return name;
    }

    public List<String> markers() {
        return markers;
    }
}
