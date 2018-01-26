package com.hazelcast.jet.demo;

import com.hazelcast.console.ConsoleApp;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;

public class DemoCLI {

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "log4j");
        JetInstance jet = Jet.newJetInstance();

        ConsoleApp app = new ConsoleApp(jet.getHazelcastInstance());
        app.start();
    }

}
