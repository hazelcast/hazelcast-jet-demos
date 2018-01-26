/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.fileReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rlab.cache.ContractInfoRepository;
import com.rlab.entity.ContractInfo;



public class ContractInfoLoader extends CSVFileReader {

	
	public ContractInfoRepository ciR;
	
	
	public ContractInfoLoader(String fileName){
		super(fileName);
	}
	
	public void setRepository(ContractInfoRepository ciR){
		this.ciR=ciR;
	}
	
	@Override
	public void processLine(String[] fields) {
		ContractInfo ci = new ContractInfo(Integer.parseInt(fields[0]),
				                           Integer.parseInt(fields[1]),
				                           Integer.parseInt(fields[2]),
				                           Integer.parseInt(fields[3]),
				                           fields[4],
				                           Integer.parseInt(fields[5]),
				                           fields[6]);
		ciR.add(ci, ci.getKey());

	}

}
