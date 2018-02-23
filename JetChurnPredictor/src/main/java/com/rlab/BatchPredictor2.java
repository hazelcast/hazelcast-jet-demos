package com.rlab;

import static java.lang.Runtime.getRuntime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import com.rlab.entity.AllContractInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ModelEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.Edge.from;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.IList;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.KafkaSources;
import com.hazelcast.jet.Pipeline;
import com.hazelcast.jet.Sinks;
import com.hazelcast.jet.Sources;
import com.hazelcast.jet.config.InstanceConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.SinkProcessors;
import com.hazelcast.jet.core.processor.SourceProcessors;
import com.hazelcast.jet.stream.IStreamMap;
import com.rlab.cache.ContractInfoRepository;
import com.rlab.cache.CustomerUsageRepository;
import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;
import com.rlab.imdg.member.ServerMember;
import com.rlab.jet.kafka.Sender;
import  com.rlab.jet.scoring.JPMMLUtils;
import com.rlab.jet.scoring.MLException;
import com.rlab.kafka.message.KMessage;
/**
 * A sample which consumes two Kafka topics and writes
 * the received items to an {@code IMap}.
 **/

@Component
public class BatchPredictor2 implements CommandLineRunner  {

	private static final Logger logger = LoggerFactory.getLogger(KickOff.class);

	@Autowired
	private final CacheManager cacheManager =null;
	
	

	@Autowired
	ProfileStarter ps;
	
	@Autowired
	ProjectConfig pConf;
	@Autowired
	ContractInfoRepository  ciRep;

	@Autowired
	CustomerUsageRepository cuRep;
	IList<AllContractInfo> inputList =null;
	private static final String SINK_NAME = "sink";

	private static final String INPUT_LIST = "inputList";
	private static final String RESULT_LIST = "resultList";

	private IList<String> phoneNoslist = ServerMember.hazelcastInstance().getList("PhoneNos");
	
	ObjectMapper jom = new ObjectMapper();
	
	@Autowired
	private Sender sender;

	public static void main(String[] args) throws Exception {
		System.setProperty("hazelcast.logging.type", "log4j");
		new BatchPredictor2().run();
	}


	private  Pipeline buildPipeline() {

		Pipeline p =null;
		/*try{
			p = Pipeline.create();
			p.drawFrom(Sources.<AllContractInfo>list(INPUT_LIST))
			//.map(i -> i.trim())
			.map(i -> i.predictChurn(i.getCi(),i.getCud(),modelEvaluator) )
			.drainTo(Sinks.list(RESULT_LIST));
		}catch (Exception e){
			e.printStackTrace();
		}*/
		return p;

	}
	
	private DAG buildDAG(){
		DAG dag = new DAG();
		//dag.newVertex(name, metaSupplier)
		 Vertex source = dag.newVertex("source", SourceProcessors.<AllContractInfo>readListP(INPUT_LIST)).localParallelism(1);
		  Vertex transform =  dag.newVertex("transform",ScoringProcessor.streamScoreP()); 
		  Vertex sink = dag.newVertex("sink", SinkProcessors.writeListP(RESULT_LIST));

          dag.edge(between(source, transform));
          dag.edge(between(transform, sink));

         return dag;
				
		
		
		
	}

	
	public void loadCallData(String... strings) throws Exception {
		logger.info("\n\n" + "=========================================================\n"
				+ "Using cache manager: " + cacheManager.getClass().getName() + "\n"
				+ "=========================================================\n\n");
		if(pConf.getProperty("loadData","false").equals("true")){
			logger.info("Loading Data .... ");
		   ps.start();
		   logger.info("Finished Loading Data .... ");
		   
			}else{
			logger.info("Starting up WITHOUT Loading Data .... ");
			}
		}

	private void startProcess(){
		JetConfig cfg = new JetConfig();
		cfg.setInstanceConfig(new InstanceConfig().setCooperativeThreadCount(
				Math.max(1, getRuntime().availableProcessors() / 2)));

		try {
			JetInstance instance = Jet.newJetInstance(cfg);
			inputList = instance.getList(INPUT_LIST);
			
			{
				for (String pNo: phoneNoslist){
					inputList.add(new AllContractInfo(ciRep.findByKey(pNo),cuRep.findByKey(pNo)));
				}
			}
			
	
			long startT = System.currentTimeMillis();
			try {
				
				instance.newJob(this.buildDAG()).execute().get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        
			long endTime = System.currentTimeMillis();
			IList<KMessage> outputList = instance.getList(RESULT_LIST);
			//System.out.println("Result list items: " + new ArrayList<>(outputList));
		
			ArrayList<KMessage>  slist= new ArrayList<KMessage> (outputList);
			slist.stream()
			.forEach(item->System.out.println(((KMessage)item).toString()));
			
			System.out.println("===============================================================");
			System.out.println(inputList.size() +" mobile contracts analyzed for churn in "+(endTime-startT)+" milliseconds");
			System.out.println("===============================================================");
		} finally {
			//Jet.shutdownAll();
		}
	}



	private static Properties props(String... kvs) {
		final Properties props = new Properties();
		for (int i = 0; i < kvs.length;) {
			props.setProperty(kvs[i++], kvs[i++]);
		}
		return props;
	}

	@Override
	public void run(String... arg0) throws Exception {
		// TODO Auto-generated method stub
		loadCallData();
		startProcess();
	}

	
	
	
	
}
