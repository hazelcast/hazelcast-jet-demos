package com.hazelcast.jet.demo.support;

import java.io.IOException;
import java.util.Properties;

import static com.hazelcast.jet.impl.util.ExceptionUtil.rethrow;

public class Util {
    public static Properties loadCredentials() {
        Properties tokens = new Properties();
        try {
            tokens.load(Thread.currentThread().getContextClassLoader()
                              .getResourceAsStream("twitter-security.properties"));
        } catch (IOException e) {
            throw rethrow(e);
        }
        return tokens;
    }
}
