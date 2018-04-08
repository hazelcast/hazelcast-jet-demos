package com.hazelcast.jet.demo.util;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.demo.common.CoinDefs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import static com.hazelcast.jet.demo.common.CoinDefs.COIN_MAP;
import static com.hazelcast.jet.demo.common.CoinDefs.SYMBOL;
import static com.hazelcast.util.ExceptionUtil.rethrow;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Contains utility methods to load properties files and starting/stopping console printer threads
 */
public class Util {

    public static final String MAP_NAME_30_SECONDS = "map30Seconds";
    public static final String MAP_NAME_1_MINUTE = "map1Min";
    public static final String MAP_NAME_5_MINUTE = "map5Min";

    private static volatile boolean running = true;
    private static final long PRINT_INTERNAL_MILLIS = 10_000L;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private Util() {
    }

    public static void startConsolePrinterThread(JetInstance jet) {
        new Thread(() -> {
            Map<String, Tuple2<Double, Long>> map30secs = jet.getMap(MAP_NAME_30_SECONDS);
            Map<String, Tuple2<Double, Long>> map1min = jet.getMap(MAP_NAME_1_MINUTE);
            Map<String, Tuple2<Double, Long>> map5min = jet.getMap(MAP_NAME_5_MINUTE);

            while (running) {
                Set<String> coins = new HashSet<>();

                if (map30secs.isEmpty()) {
                    continue;
                }

                coins.addAll(map30secs.keySet());
                coins.addAll(map1min.keySet());
                coins.addAll(map5min.keySet());

                System.out.println("\n");
                System.out.println("/----------------+---------------+---------------+----------------\\");
                System.out.println("|                |          Sentiment (tweet count)               |");
                System.out.println("| Coin           | Last 30 sec   | Last minute   | Last 5 minutes |");
                System.out.println("|----------------+---------------+---------------+----------------|");
                coins.forEach((coin) ->
                        System.out.format("| %s  | %s | %s | %s  |%n",
                            coinName(coin), format(map30secs.get(coin)), format(map1min.get(coin)), format(map5min.get(coin))));
                System.out.println("\\----------------+---------------+---------------+----------------/");

                LockSupport.parkNanos(MILLISECONDS.toNanos(PRINT_INTERNAL_MILLIS));
            }
        }).start();
    }

    /**
     * Gets the full name given the code and pads it to a length of 16 characters
     */
    private static Object coinName(String coin) {
        String name = COIN_MAP.get(coin).get(SYMBOL);
        for (int i = name.length(); i < 13; i++){
            name = name.concat(" ");
        }
        return name;
    }

    public static void stopConsolePrinterThread() {
        running = false;
    }


    public static List<String> loadTerms() {
        List<String> terms = new ArrayList<>();
        COIN_MAP.forEach((key, value) -> {
            terms.add(key);
            terms.addAll(value);
        });
        return terms;
    }

    public static Properties loadProperties() {
        Properties tokens = new Properties();
        try {
            tokens.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            throw rethrow(e);
        }
        return tokens;
    }

    private static String format(Tuple2<Double, Long> t) {
        if (t == null || t.f1() == 0) {
            return "             ";
        }
        String color = t.f0() > 0 ? ANSI_GREEN : t.f0() == 0 ? ANSI_YELLOW : ANSI_RED;
        return String.format("%s%7.4f (%3d)%s", color, t.f0(), t.f1(), ANSI_RESET);
    }

}
