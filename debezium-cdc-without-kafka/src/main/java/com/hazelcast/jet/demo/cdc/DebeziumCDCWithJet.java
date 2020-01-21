package com.hazelcast.jet.demo.cdc;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.contrib.debezium.DebeziumSources;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.impl.JetBootstrap;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import io.debezium.config.Configuration;
import org.apache.kafka.connect.data.Values;

/**
 * Simple Jet pipeline which consumes CDC events from MySQL via Debezium
 * Connector and prints the objects to the standard output in the string format.
 */
public class DebeziumCDCWithJet {

    public static void main(String[] args) {
        JetInstance jet = JetBootstrap.getInstance();

        Configuration configuration = Configuration
                .create()
                .with("name", "mysql-demo-connector")
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                /* begin connector properties */
                .with("database.hostname", "mysql")
                .with("database.port", "3306")
                .with("database.user", "debezium")
                .with("database.password", "dbz")
                .with("database.server.id", "184054")
                .with("database.server.name", "dbserver1")
                .with("database.whitelist", "inventory")
                .with("database.history.hazelcast.list.name", "test")
                .with("snapshot.mode", "schema_only")
                .build();

        Pipeline p = Pipeline.create();

        p.readFrom(DebeziumSources.cdc(configuration))
         .withoutTimestamps()
         .map(sourceRecord -> {
             String keyString = Values.convertToString(sourceRecord.keySchema(), sourceRecord.key());
             String valueString = Values.convertToString(sourceRecord.valueSchema(), sourceRecord.value());
             return Tuple2.tuple2(keyString, valueString);
         })
         .writeTo(Sinks.logger());

        jet.newJob(p).join();
    }
}
