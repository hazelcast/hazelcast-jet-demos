package com.hazelcast.jet.demos.traintrack;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyEnrichAndReformatFnTest {

	static final long NOW = 987654321;
	
	static MyEnrichAndReformatFn myEnrichAndReformatFn;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		myEnrichAndReformatFn = new MyEnrichAndReformatFn(NOW);
	}

	@Test
	public void json_parse() throws JSONException {
		long offset = 789;
		String input = "123,456," + offset;
		
		String output = myEnrichAndReformatFn._process(input);
		
		log.info("{} -> {}", input, output);
		
		assertThat("output", output, not(nullValue()));
		
		JSONObject jsonObject = new JSONObject(output);
		
		String latitude = jsonObject.getString("latitude");
		assertThat("latitude", latitude, not(nullValue()));
		assertThat("latitude", latitude, is(equalTo("123")));
		
		String longitude = jsonObject.getString("longitude");
		assertThat("longitude", longitude, not(nullValue()));
		assertThat("longitude", longitude, is(equalTo("456")));
		
		String timestamp = jsonObject.getString("timestamp");
		assertThat("timestamp", timestamp, not(nullValue()));
		assertThat("timestamp", timestamp, is(equalTo(String.valueOf(NOW + offset * 1000))));
	}

}
