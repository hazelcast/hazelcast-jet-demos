package com.rlab;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;


@EnableCaching
@SpringBootApplication
public class Application  { 
	


	
	public static void main(String[] args) {
		new SpringApplicationBuilder().sources(Application.class)
		.profiles("app").run(args);
		//new SpringApplicationBuilder().sources(Application.class).run(args);
		// SpringApplication.run(Application.class, args);
	}

	@Bean
    @Profile("app")
    HazelcastInstance hazelcastInstance() {
        // for client HazelcastInstance LocalMapStatistics will not available
        return HazelcastClient.newHazelcastClient();
         //return Hazelcast.newHazelcastInstance();
    }
}
