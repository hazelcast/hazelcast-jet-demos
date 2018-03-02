package common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinDefs {

    public static Map<String, List<String>> COIN_MAP = new HashMap<>();

    static {
        COIN_MAP.put("BTC", Arrays.asList("bitcoin", "#btc", "#bitcoin"));
        COIN_MAP.put("ETH", Arrays.asList("ether", "ethereum", "#eth", "#ether", "#ethereum"));
        COIN_MAP.put("XRP", Arrays.asList("ripple", "#xrp", "#ripple"));
        COIN_MAP.put("BCH", Arrays.asList("bitcoin cash", "#bitcoincash", "#bch"));
        COIN_MAP.put("ADA", Arrays.asList("cardano", "#ada", "#cardano"));
        COIN_MAP.put("LTC", Arrays.asList("litecoin", "#ltc", "#litecoin"));
        COIN_MAP.put("XLM", Arrays.asList("stellar", "#xlm", "#steller"));
        COIN_MAP.put("XEM", Arrays.asList("NEM", "#xem", "#nem"));
        COIN_MAP.put("EOS", Arrays.asList("EOS", "#eos"));
    }
}
