import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinDefs {

    static Map<String, List<String>> coinMap = new HashMap<>();

    static {
        coinMap.put("BTC", Arrays.asList("bitcoin", "#btc", "#bitcoin"));
        coinMap.put("ETH", Arrays.asList("ether", "ethereum", "#eth", "#ether", "#ethereum"));
        coinMap.put("XRP", Arrays.asList("ripple", "#xrp", "#ripple"));
        coinMap.put("BCH", Arrays.asList("bitcoin cash", "#bitcoincash", "#bch"));
        coinMap.put("ADA", Arrays.asList("cardano", "#ada", "#cardano"));
        coinMap.put("LTC", Arrays.asList("litecoin", "#ltc", "#litecoin"));
        coinMap.put("XLM", Arrays.asList("stellar", "#xlm", "#steller"));
        coinMap.put("XEM", Arrays.asList("NEM", "#xem", "#nem"));
        coinMap.put("EOS", Arrays.asList("EOS", "#eos"));
    }

    static List<String> redditNames = Arrays
            .asList("Bitcoin", "Etherum", "Ripple", "Bitcoincash", "Cardano", "Litecoin", "Stellar", "nem", "eos");

}
