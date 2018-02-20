package com.rlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class JetScoringEngineApplication {

	public static void main(String[] args) {
	//	SpringApplication.run(JetScoringEngineApplication.class, args);
		
		new SpringApplicationBuilder().sources(JetScoringEngineApplication.class)
		.profiles("app").run(args);
	}
}
