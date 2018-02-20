/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:config.properties")
//@PropertySource("file:${app.home}/app.properties")
//@PropertySource(value="classpath:missing.properties", ignoreResourceNotFound=true)
public class ProjectConfig {
	
	@Autowired
	private Environment env;

	public String getProperty(String propertyName){
		return env.getProperty(propertyName);
	}
	
	public String getProperty(String propertyName,String def){
		String res = env.getProperty(propertyName);
		if (res==null)
			return def;
		return res.trim();
	}

}
