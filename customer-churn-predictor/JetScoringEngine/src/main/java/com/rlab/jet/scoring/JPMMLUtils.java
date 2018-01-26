package com.rlab.jet.scoring;



import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ModelField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.evaluator.tree.NodeScoreDistribution;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.PMMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.RangeSet;
import com.rlab.kafka.message.KMessage;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JPMML util class that provides useful functions such as reading and writing pmml documents.
 *
 * @author Lyndon Adams
 * @since  Apr, 2014
 */
public final class JPMMLUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger( "JPMMLUtils" );
   // private static   ModelEvaluator<?> modelEvaluator;
	private JPMMLUtils(){
	}
	

	/**
	 * Load a PMML model from the file system.
	 *
	 * @param file
	 * @return
	 * @throws MLException
	 */
	public static final PMML loadModel(final String file) throws MLException {
		PMML pmml = null;

		File inputFilePath = new File( file ) ;

		try{
			pmml=readPMML(inputFilePath);
		} catch(   Exception  e) {
			LOGGER.error(e.toString());
			throw new MLException( e);
		}
		return pmml;
	}
	
	public static final Map<FieldName, ?> evaluate ( ModelEvaluator<?> modelEvaluator, Map<String,?> imap  ){
		LOGGER.info("Evaluation Start ==>");
	
		    List<InputField> inputFields = modelEvaluator.getInputFields();
		    Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
		    //List<Map<FieldName, ?>> outputRecords = new ArrayList<>();
		    

				for(InputField inputField : inputFields){
					FieldName name = inputField.getName();

					FieldValue value = EvaluatorUtil.prepare(inputField, imap.get(name.getValue()));

					arguments.put(name, value);
				}

				Map<FieldName, ?> result = modelEvaluator.evaluate(arguments);
           
				LOGGER.info("Result :: "+ result );
				
		    
				LOGGER.info("Evaluation END <==");
				return result;
		    
	}
	
	
      public static final KMessage evaluate(int reqID, ModelEvaluator<?> modelEvaluator, Map<String,?> imap ){
    	  
       	  Map<FieldName, ?> result = evaluate(modelEvaluator,imap);
    	  if(result == null)
    		  return null;
    	  KMessage res = new KMessage();
    	  res.setId(reqID);
    	  res.setMessageType(1);
    	  NodeScoreDistribution nsd = (NodeScoreDistribution) result.get(new FieldName("Churn"));
    	  
    	 res.setAttribute("Result", nsd.getResult());
    	 res.setAttribute("P0", new BigDecimal(nsd.getValue("0").doubleValue() * 100).setScale(2, BigDecimal.ROUND_DOWN).doubleValue());
    	 res.setAttribute("P1", new BigDecimal(nsd.getValue("1").doubleValue() * 100).setScale(2, BigDecimal.ROUND_DOWN).doubleValue());
    	  //result.
    	  return res;
      }
	

	public static final ModelEvaluator<?>  getVerifiedEvaluator(PMML pmml) throws ClassNotFoundException, ReflectiveOperationException{

		ModelEvaluatorFactory modelEvaluatorFactory = (ModelEvaluatorFactory)newInstance(Class.forName(ModelEvaluatorFactory.class.getName()));

		ValueFactoryFactory valueFactoryFactory = (ValueFactoryFactory)newInstance(Class.forName(ValueFactoryFactory.class.getName()));
		modelEvaluatorFactory.setValueFactoryFactory(valueFactoryFactory);

		ModelEvaluator<?>  evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
		evaluator.verify();
		//modelEvaluator=evaluator;
		return evaluator;
	}


	public static final void analyseInputFields(Evaluator evaluator){
		LOGGER.info(" ===>> Analyzing Input Fields : Model = "+evaluator.getSummary() );
		List<InputField> inputFields = evaluator.getInputFields();
		for(InputField inputField : inputFields){
			org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField)inputField.getField();
			org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();
			printDataField(pmmlDataField,pmmlMiningField);
			//printDFCode(pmmlDataField,pmmlMiningField);
		}
		
		LOGGER.info("<< ===>>");

	}
	
	public static final void analyseTargetFields(Evaluator evaluator){
		LOGGER.info(" ===>> Analyzing Target  Fields : Model = "+evaluator.getSummary() );
		List<TargetField> tFields = evaluator.getTargetFields();
		for(TargetField tField : tFields){
			org.dmg.pmml.DataField pmmlDataField = tField.getDataField();
			org.dmg.pmml.MiningField pmmlMiningField = tField.getMiningField();
			printDataField(pmmlDataField,pmmlMiningField);
		}
		
		LOGGER.info("<< ===>>");

	}

	
	public static final void analyseOutputFields(Evaluator evaluator){
		LOGGER.info(" ===>> Analyzing Target  Fields : Model = "+evaluator.getSummary() );
		List<org.jpmml.evaluator.OutputField> oFields = evaluator.getOutputFields();
		for(org.jpmml.evaluator.OutputField oField : oFields){
			org.dmg.pmml.OutputField pmmlOutputField = oField.getOutputField();
			printOutputField(pmmlOutputField);
		}
		
		LOGGER.info("<< ===>>");

	}


	private static void printDataField(DataField  pmmlDataField,MiningField mField){
		
		LOGGER.info(" Data Field :"+ pmmlDataField.getName() +" | "+pmmlDataField.getDataType().name()+ " | "+pmmlDataField.getOpType().name() );
		
		if(mField != null)
		    LOGGER.info(" Mining Field :"+ mField.getName()  );
		
		 org.dmg.pmml.OpType opType = pmmlDataField.getOpType();
		//LOGGER.info(" Data Type IF : "+ opType.name() + " | " +  opType.value() + " | "+opType.toString() );
		
		switch(opType){
		case CONTINUOUS:{
			RangeSet<Double> validArgumentRanges = FieldValueUtil.getValidRanges(pmmlDataField);
			LOGGER.info("Valid Range : "+validArgumentRanges.asDescendingSetOfRanges().toString());
			break;
		}
		case CATEGORICAL:
		case ORDINAL:
			List<Value> validArgumentValues = FieldValueUtil.getValidValues(pmmlDataField);
			LOGGER.info("Valid Arguments : "+validArgumentValues.toString());
			break;
		default:
			break;
		}
	}
	
	
private static void printDFCode(DataField  pmmlDataField,MiningField mField){
		
		LOGGER.info("imap.put(\""+ pmmlDataField.getName() +"\", \"\" ); " );
				
	}
private static void printOutputField(org.dmg.pmml.OutputField  oField){
		
		LOGGER.info(" OutPut Field :"+ oField.getName() +" | "+oField.getDataType().name()+ " | "+oField.getOpType().name() );
		
		
	}
	/**
	 * Build a feature set to use against a PMML model.
	 *
	 * @param evaluator
	 * @param requiredModelFeatures
	 * @param data
	 * @return  Map<FieldName, FieldValue>
	 */
	public static final Map<FieldName, FieldValue> buildFeatureSet(Evaluator evaluator, List<FieldName> requiredModelFeatures, Object[] data){

		Map<FieldName, FieldValue> features = new LinkedHashMap<>();

		for(int i=0; i<requiredModelFeatures.size(); i++){
			FieldName fieldName= requiredModelFeatures.get( i);
			// FieldValue value = evaluator.prepare(fieldName, data[i]);
			//features.put( fieldName, value);
		}
		return features;
	}

	static
	public PMML readPMML(File file) throws Exception {

		try(InputStream is = new FileInputStream(file)){
			return PMMLUtil.unmarshal(is);
		}
	}

	static
	public void writePMML(PMML pmml, File file) throws Exception {

		try(OutputStream os = new FileOutputStream(file)){
			PMMLUtil.marshal(pmml, os);
		}
	}

	static
	public CsvUtil.Table readTable(File file, String separator) throws IOException {

		try(InputStream is = new FileInputStream(file)){
			return CsvUtil.readTable(is, separator);
		}
	}

	static
	public void writeTable(CsvUtil.Table table, File file) throws IOException {

		try(OutputStream os = new FileOutputStream(file)){
			CsvUtil.writeTable(table, os);
		}
	}

	static
	public Object newInstance(Class<?> clazz) throws ReflectiveOperationException {
		Method newInstanceMethod = clazz.getDeclaredMethod("newInstance");

		return newInstanceMethod.invoke(null);
	}

}