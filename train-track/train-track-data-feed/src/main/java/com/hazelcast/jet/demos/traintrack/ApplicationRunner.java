package com.hazelcast.jet.demos.traintrack;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Write the GPS data to a file named "{@code beam-input}",
 * one per second.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationRunner implements CommandLineRunner {

    private static final String FILE_NAME = "beam-input";

    @Override
    public void run(String... args) throws Exception {

    	File file = new File(FILE_NAME);
        try (PrintWriter printWriter = new PrintWriter(file);) {
        	
            Stream.of(FrecciaRossa.POINTS)
            .forEach(trio -> {
                try {
                    TimeUnit.SECONDS.sleep(1L);
                } catch (InterruptedException ignored) {
                    log.info("Interrupted");
                }
                String csv = trio[0] + "," + trio[1] + "," + trio[2]; 
                log.info(csv);
                printWriter.println(csv);
                printWriter.flush();
            });
        }
 	   
    }

}
