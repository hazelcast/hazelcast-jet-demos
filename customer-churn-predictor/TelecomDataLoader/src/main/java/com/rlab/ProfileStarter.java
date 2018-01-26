/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.rlab.cache.ContractInfoRepository;
import com.rlab.cache.CustomerUsageRepository;
import com.rlab.fileReader.ContractInfoLoader;
import com.rlab.fileReader.CustomerUsageLoader;

@Component
@Profile("app")
public class ProfileStarter {
	
	private ContractInfoRepository cir;
	private CustomerUsageRepository cur;
	
	public ProfileStarter(ContractInfoRepository cir, CustomerUsageRepository cur){
		this.cir=cir;
		this.cur=cur;
		
	}
	
	public void start(){
		ContractInfoLoader cil = new ContractInfoLoader("src\\main\\resources\\ContractData.csv");
		cil.setRepository(cir);
		cil.startLoading();
		
		CustomerUsageLoader cul = new CustomerUsageLoader("src\\main\\resources\\CallsData.csv");
		cul.setRepository(cur);
		cul.startLoading();
	}
}
