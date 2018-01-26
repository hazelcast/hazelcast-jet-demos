/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class KickOff implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(KickOff.class);

	private final CacheManager cacheManager;
	
	

	@Autowired
	ProfileStarter ps;
	
	@Autowired
	ProjectConfig pConf;
	
	public KickOff(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public void run(String... strings) throws Exception {
		logger.info("\n\n" + "=========================================================\n"
				+ "Using cache manager: " + this.cacheManager.getClass().getName() + "\n"
				+ "=========================================================\n\n");
		if(pConf.getProperty("loadData","false").equals("true")){
			logger.info("Loading Data .... ");
		   ps.start();
		}else{
			logger.info("Starting up WITHOUT Loading Data .... ");
		}
	}

}
