/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.fileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.rlab.jet.scoring.JPMMLUtils;

public abstract class CSVFileReader {
	
	private String filename;
	protected CSVFileReader(String filename){
		this.filename=filename;
	}
	
	public void startLoading(){
		loadFile();
	}
	
	private void loadFile(){
		String line = "";
        String cvsSplitBy = ",";
        
        File f = new File(".");
        System.out.println(f.getAbsolutePath());
        
        ClassLoader classLoader = CSVFileReader.class.getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            //skip header 
        	br.readLine();
        	
            while ((line = br.readLine()) != null) {
            	
                String[] fields = line.split(cvsSplitBy);
                processLine(fields);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	
	public abstract void processLine(String[] fields);
    
	public static void main(String[] args) {

      
        

    }

}
