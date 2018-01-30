package com.rlab.jet;

import static java.lang.Runtime.getRuntime;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ModelEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.KafkaSources;
import com.hazelcast.jet.Pipeline;
import com.hazelcast.jet.Sinks;
import com.hazelcast.jet.config.InstanceConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.stream.IStreamMap;
import com.rlab.jet.kafka.Sender;
import  com.rlab.jet.scoring.JPMMLUtils;
import com.rlab.jet.scoring.MLException;
/**
 * A sample which consumes two Kafka topics and writes
 * the received items to an {@code IMap}.
 **/

@Component
public class Scorer implements CommandLineRunner  {

	private static final int MESSAGE_COUNT_PER_TOPIC = 1_000_000;
	private static final String BOOTSTRAP_SERVERS = "localhost:9092";
	private static final String AUTO_OFFSET_RESET = "earliest";

	private static final String SINK_NAME = "sink";
	ObjectMapper jom = new ObjectMapper();
	ModelEvaluator<?> modelEvaluator;

	@Autowired
	private Sender sender;

	public static void main(String[] args) throws Exception {
		System.setProperty("hazelcast.logging.type", "log4j");
		new Scorer().run();
	}

	private void initPMMLModel(){
		PMML pmml;
		try {
			//pmml = JPMMLUtils.loadModel("src\\main\\resources\\churnPmmlModel.pmml");
			pmml = JPMMLUtils.loadModel("./src/main/resources/churnPmmlModel.pmml");
			modelEvaluator = JPMMLUtils.getVerifiedEvaluator(pmml);
			Model model = modelEvaluator.getModel();
		} catch (MLException | ReflectiveOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Pipeline buildPipeline() {
		Pipeline p = Pipeline.create();
		p.drawFrom(KafkaSources.kafka(brokerProperties(), "t1"))
		.drainTo(Sinks.map(SINK_NAME))
		;

		return p;
	}

	private void run() throws Exception {
		startProcess();
	}

	private void startProcess(){
		JetConfig cfg = new JetConfig();
		cfg.setInstanceConfig(new InstanceConfig().setCooperativeThreadCount(
				Math.max(1, getRuntime().availableProcessors() / 2)));

		try {
			initPMMLModel();
			JetInstance instance = Jet.newJetInstance(cfg);
			//Jet.newJetInstance(cfg);
			IStreamMap<String,String> sinkMap = instance.getMap(SINK_NAME);


			//.map(JsonUtils::readJson)

			Pipeline p = buildPipeline();

			long start = System.nanoTime();
			Job job = instance.newJob(p);


			while (true) {
				int mapSize = sinkMap.size();
				if(mapSize != 0){



					ArrayList<String>  slist= new ArrayList<String> (sinkMap.values());
					sinkMap.clear();
					slist.stream().map(JsonUtils::readJson)
					.forEach(item->sender.send("t2",JPMMLUtils.evaluate(item.getId(),modelEvaluator,item.getAttributes())));
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} finally {
			Jet.shutdownAll();
		}
	}



	private static Properties brokerProperties() {
		return props(
				"bootstrap.servers", BOOTSTRAP_SERVERS,
				"key.deserializer", StringDeserializer.class.getCanonicalName(),
				"value.deserializer",StringDeserializer.class.getCanonicalName(),
				"auto.offset.reset", AUTO_OFFSET_RESET);
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
		startProcess();
	}
}
