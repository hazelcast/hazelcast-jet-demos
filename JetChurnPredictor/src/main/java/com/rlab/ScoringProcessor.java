package com.rlab;


import static com.hazelcast.jet.Sources.fromProcessor;
import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.core.ProcessorMetaSupplier.dontParallelize;
import static com.hazelcast.jet.impl.util.Util.uncheckCall;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ModelEvaluator;

import com.hazelcast.core.IList;
import com.hazelcast.jet.Source;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.rlab.entity.AllContractInfo;
import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;
import com.rlab.jet.scoring.JPMMLUtils;
import com.rlab.jet.scoring.MLException;
import com.rlab.kafka.message.KMessage;

/**
 * Polls the <a href="https://www.adsbexchange.com">ADS-B Exchange</a> HTTP API
 * for flight data. The API will be polled every {@code pollIntervalMillis} milliseconds.
 *
 * After a successful poll, this source filters out aircrafts which are missing registration number
 * and position timestamp. It will also records the latest position timestamp of the aircrafts so if
 * there are no update for an aircraft it will not be emitted from this source.
 */
public class ScoringProcessor extends AbstractProcessor {

    private Traverser<KMessage> traverser;
    private long lastPoll;
    
   
    ModelEvaluator<?> modelEvaluator;

    public ScoringProcessor( ) {
         
            initPMMLModel();
    }   
    
	private void initPMMLModel(){
		PMML pmml;
		try {
			pmml = JPMMLUtils.loadModel("src/main/resources/churnPmmlModel.pmml");
			modelEvaluator = JPMMLUtils.getVerifiedEvaluator(pmml);
			Model model = modelEvaluator.getModel();
		} catch (MLException | ReflectiveOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

 
    
    @Override
    public boolean  tryProcess(int i , Object message){
      AllContractInfo  ci = (AllContractInfo) message;
      KMessage kmsg= this.predictChurn(ci.getCi(),ci.getCud());
      return emitFromTraverser(Traversers.traverseIterable(Arrays.asList(kmsg)));
      
    }

    @Override
    public boolean isCooperative() {
        return false;
    }


   
    static int count=0;
	public  KMessage predictChurn(ContractInfo ci,CustomerUsageDetails cud)  {
		KMessage kmsg = new KMessage();
		kmsg.setId(count++);
		// 1 = Request 2 = Response
		kmsg.setMessageType(1);
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
		KMessage ret =JPMMLUtils.evaluate(kmsg.getId(),modelEvaluator,kmsg.getAttributes());
		return ret;
	}

    public static ProcessorMetaSupplier streamScoreP( ) {
        return dontParallelize(() -> new ScoringProcessor());
    }

    public static Source<AllContractInfo> streamScore( ) {
        return fromProcessor("streamScore", streamScoreP());
    }

}
