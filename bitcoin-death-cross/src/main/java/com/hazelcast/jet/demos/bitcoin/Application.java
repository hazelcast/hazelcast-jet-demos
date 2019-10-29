package com.hazelcast.jet.demos.bitcoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Use <a href="http://spring.io/projects/spring-boot">Spring Boot</a>
 * to manage this application.
 * </p>
 */
@SpringBootApplication
public class Application {

	/**
	 * <p><b>java.awt.headless</b> Chart plotting requires
	 * this, there is no controlling parent window to attach to.
	 * </p>
	 */
    static {
        System.setProperty("java.awt.headless", "false");
    }
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
