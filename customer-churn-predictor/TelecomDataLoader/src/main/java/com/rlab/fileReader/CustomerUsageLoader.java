/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.fileReader;

import com.rlab.cache.ContractInfoRepository;
import com.rlab.cache.CustomerUsageRepository;
import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;



public class CustomerUsageLoader extends CSVFileReader {

	
	public CustomerUsageRepository ciR;
	
	
	public CustomerUsageLoader(String fileName){
		super(fileName);
	}
	
	public void setRepository(CustomerUsageRepository ciR){
		this.ciR=ciR;
	}
	
	
	
	
	@Override
	public void processLine(String[] fields) {
		CustomerUsageDetails ci = new CustomerUsageDetails(Integer.parseInt(fields[0]),
				                           Float.parseFloat(fields[1]),
				                           Float.parseFloat(fields[2]),
				                           Float.parseFloat(fields[3]),
				                           Float.parseFloat(fields[4]),
				                           
				                           Integer.parseInt(fields[5]),
				                           
				                           Integer.parseInt(fields[6]),
				                           Float.parseFloat(fields[7]),
				                           
				                           Integer.parseInt(fields[8]),
				                           Float.parseFloat(fields[9]),
				                           
				                           Integer.parseInt(fields[10]),
				                           Float.parseFloat(fields[11]),
				                           
				                           Integer.parseInt(fields[12]),
				                           Float.parseFloat(fields[13]),
				                           
				                           fields[14],
				                        	fields[15]
				                           );
		ciR.add(ci, ci.getKey());

	}

}
