
#  PMML Model Importer and Executor
### This Sample  
       1. Starts a Hazelcast IMDG Member
       2. Starts a SpringBoot app , loads Call data and contract data to IMDG
       3. Starts a Jet instance .
       4. Loads PMML Model 
       5. Streams  a batch of phone numbers to Jet  which creates the PMML prediction requests 
       6. Uses embedded JPMML Engine to predict churn
       7. return results back to console with scoring performance stats 
       
       
       ITS EASY TO START  launch
         java com.rlab.JetScoringEngineApplication
         
         check log file.
         
         Other UI version will follow  
  
