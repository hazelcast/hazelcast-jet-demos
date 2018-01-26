package com.rlab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	@Autowired
	private CacheManager cacheManager;

	

	@Test
	public void validateCache() {
		Cache contractInfo = this.cacheManager.getCache("contractInfo");
		assertThat(contractInfo).isNotNull();
	}
}
