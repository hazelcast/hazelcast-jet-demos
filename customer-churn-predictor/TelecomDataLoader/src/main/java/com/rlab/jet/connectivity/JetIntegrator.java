package com.rlab.jet.connectivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;
import com.rlab.kafka.Sender;
import com.rlab.kafka.message.KMessage;


@Scope(value = "singleton")
@Component	

public class JetIntegrator {

	private static final Logger log = LoggerFactory.getLogger("JetIntegrator");
	
	//static IEventService apamaEventService;
    static int count;
	static Map<Integer, CountDownLatch> responseEventLatches = new HashMap<Integer, CountDownLatch>();
	static ConcurrentMap<Integer, KMessage> responseEvents = new ConcurrentHashMap<Integer, KMessage>();
	
	@Autowired
	Sender sender;
	 
	private JetIntegrator() {
	}
	
	
	public  KMessage predictChurn(ContractInfo ci,CustomerUsageDetails cud) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		log.info("cI ### "+ci.toString());
		log.info("CUD ### "+ cud.toString());
		KMessage kmsg = new KMessage();
		kmsg.setId(count++);
		// 1 = Request 2 = Response
		kmsg.setMessageType(1);
		Map<String,Object> attributes = new HashMap<String,Object>();
		kmsg.setAttribute("VMail Message", cud.getvMailMessage() ); 
    	kmsg.setAttribute("Day Mins", cud.getDayMins() ); 
    	kmsg.setAttribute("Eve Mins", cud.getEveMins() ); 
    	kmsg.setAttribute("Night Mins",cud.getNightMins() ); 
    	kmsg.setAttribute("Intl Mins", cud.getIntlMins() ); 
    	kmsg.setAttribute("CustServ Calls", cud.getCustomerSrvCalls() ); 
    	kmsg.setAttribute("Day Calls", cud.getDayCalls() ); 
    	kmsg.setAttribute("Day Charge", cud.getDayCharge() ); 
    	kmsg.setAttribute("Eve Calls", cud.getEveCalls() ); 
    	kmsg.setAttribute("Eve Charge", cud.getEveCharge() ); 
    	kmsg.setAttribute("Night Calls", cud.getNightCalls() ); 
    	kmsg.setAttribute("Night Charge", cud.getNightCharge() ); 
    	kmsg.setAttribute("Intl Calls", cud.getIntlCalls() ); 
    	kmsg.setAttribute("Intl Charge", cud.getIntlCharge() ); 
    	kmsg.setAttribute("Area Code", cud.getAreaCode() ); 
    	kmsg.setAttribute("Account Length", ci.getAccountLength() ); 
    	kmsg.setAttribute("Int'l Plan",ci.getIntlPlan() ); 
    	kmsg.setAttribute("VMail Plan", ci.getvMailPlan() ); 
    	kmsg.setAttribute("State", ci.getState()); 
    	kmsg.setAttribute("Phone", ci.getPhone() ); 
    	
	//	sender.send("t1","com.jet.pa.pmml.sample.SampleInput(\"Instance_1\","+ requestId  +" ,{\"VMail Message\":\""+cud.getvMailMessage()+"\",\"Day Mins\":\""+cud.getDayMins()+"\",\"Eve Mins\":\""+cud.getEveMins()+"\",\"Night Mins\":\""+cud.getNightMins()+"\",\"Intl Mins\":\""+cud.getIntlMins()+"\",\"CustServ Calls\":\""+cud.getCustomerSrvCalls()+"\",\"Day Calls\":\""+cud.getDayCalls()+"\",\"Day Charge\":\""+cud.getDayCharge()+"\",\"Eve Calls\":\""+cud.getEveCalls()+"\",\"Eve Charge\":\""+cud.getEveCharge()+"\",\"Night Calls\":\""+cud.getNightCalls()+"\",\"Night Charge\":\""+cud.getNightCharge()+"\",\"Intl Calls\":\""+cud.getIntlCalls()+"\",\"Intl Charge\":\""+cud.getIntlCharge()+"\",\"Area Code\":\""+ci.getAreaCode()+"\",\"Phone\":\""+ci.getPhone()+"\", \"Account Length\":\""+ci.getAccountLength()+"\",\"Int'l Plan\":\""+ci.getIntlPlan()+"\",\"VMail Plan\":\""+ci.getvMailPlan()+"\",\"State\":\""+ci.getState()+"\" })");
    	sender.send("t1",kmsg);
    	responseEventLatches.put(kmsg.getId(), latch);
    	latch.await();
    	if(responseEventLatches.containsKey(kmsg.getId())){
    		log.info("Got1 Response event for predictChurn = "+responseEvents.get(kmsg.getId()));
    		responseEventLatches.remove(kmsg.getId());
			return responseEvents.get(kmsg.getId());
    	}
		return null;
	}
	
	public void handlePrediction(KMessage kmsg){
		int reqID=kmsg.getId();
		if (responseEventLatches.containsKey(reqID)) {
			responseEvents.put(reqID, kmsg);
			responseEventLatches.get(reqID).countDown();
		
		}
	}
	
}
