package com.rlab.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.jpmml.evaluator.ModelEvaluator;

import com.rlab.jet.scoring.JPMMLUtils;
import com.rlab.kafka.message.KMessage;

public class AllContractInfo implements Serializable
{
	private ContractInfo ci ;
	private CustomerUsageDetails cud;
	
	
	public AllContractInfo(ContractInfo ci, CustomerUsageDetails cud) {
		super();
		this.ci = ci;
		this.cud = cud;
	}
	public ContractInfo getCi() {
		return ci;
	}
	public void setCi(ContractInfo ci) {
		this.ci = ci;
	}
	public CustomerUsageDetails getCud() {
		return cud;
	}
	public void setCud(CustomerUsageDetails cud) {
		this.cud = cud;
	}
	
	
	
}