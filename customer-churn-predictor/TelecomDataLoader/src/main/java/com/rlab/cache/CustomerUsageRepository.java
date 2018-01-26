/*
 * Copyright 2012-2015 the original author or authors.
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


package com.rlab.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;


import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;

@Component
@CacheConfig(cacheNames = "customer_usage")
public class CustomerUsageRepository {

	private static final Logger logger = LoggerFactory.getLogger(CustomerUsageRepository.class);

	@Autowired
	private CacheManager cacheManager;
	private Cache customerUsageCache;
	
	
	
	@Cacheable(cacheNames="customer_usage", key="#key" )
     public CustomerUsageDetails add(CustomerUsageDetails ci,String key){
		logger.info("Storing key :"+key+" Object :"+ ci.toString());
		return ci;
	}
	
	
	public CustomerUsageDetails findByKey(String key) {
		if(customerUsageCache == null)
			customerUsageCache = cacheManager.getCache("customer_usage");
		return (CustomerUsageDetails)customerUsageCache.get(key).get();
	}

}
