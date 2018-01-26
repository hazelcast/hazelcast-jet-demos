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

@Component
@CacheConfig(cacheNames = "contractInfo")
public class ContractInfoRepository {

	private static final Logger logger = LoggerFactory.getLogger(ContractInfoRepository.class);

	@Autowired
	private CacheManager cacheManager;
	private Cache contractInfoCache;
	
	
	
	@Cacheable(cacheNames="contractInfo", key="#key" )
     public ContractInfo add(ContractInfo ci,String key){
		logger.info("Storing key :"+key+" Object :"+ ci.toString());
		return ci;
	}
	
	
	public ContractInfo findByKey(String key) {
		if(contractInfoCache == null)
		   contractInfoCache = cacheManager.getCache("contractInfo");
		return (ContractInfo)contractInfoCache.get(key).get();
	}

}
