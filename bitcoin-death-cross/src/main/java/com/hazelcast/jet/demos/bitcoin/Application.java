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
	 * <p>Set two system properties to make the application work.
	 * </p>
	 * <ul>
	 * <li><p><b>java.awt.headless</b> Chart plotting requires
	 * this, there is no controlling parent window to attach to.
	 * </p>
	 * </li>
	 * <li><p><b>hazelcast.event.thread.count</b> Reduce the
	 * number of threads handling events down to 1. This is
	 * safe to do in this instance because not many events
	 * are generated. It is used to force one at a time
	 * updates to the graph plot.
	 * </p>
	 * </li>
	 * </ul>
	 * <p>As these can never be varied and expect this application
	 * to work properly, hard code the values rather than expose
	 * in configuration.
	 * </p>
	 */
	
    static {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("hazelcast.event.thread.count", "1");
    }
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
