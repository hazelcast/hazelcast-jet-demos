
#  PMML Model Importer and Executor
### This Sample  
       1. Starts a Hazelcast IMDG Member
       2. Starts a SpringBoot app , loads Call data and contract data to IMDG
       3. Starts a Jet instance .
       4. Loads PMML Model 
       5. Streams  a batch of phone numbers to Jet  which creates the PMML prediction requests 
       6. Uses embedded JPMML Engine to predict churn
       7. return results back to console with scoring performance stats 
       
       
 ### ITS EASY TO START  launch
         In the root folder  run command 
         > spring-boot:run
         
 ###  Monitoring
        1. Launch Management center at port 8080
        2. View cache stats in MC
 ### Results in Log file
 
 
KMessage [id=3330, messageType=1, attributes={P0=98.72, P1=1.27, Result=0}]
KMessage [id=3331, messageType=1, attributes={P0=100.0, P1=0.0, Result=0}]
KMessage [id=3332, messageType=1, attributes={P0=96.85, P1=3.14, Result=0}]
===============================================================
3333 mobile contracts analyzed for churn in 3229 milliseconds
===============================================================
         
 3333 Contract are analysed for churn using JET and JPMML Engine.
 
  [{attributes={P0=96.85, P1=3.14, Result=0}]
  
  Result : 1 = Churn | 0 = No Churn
           P1 = % chance tp Churn | P0= % chance not to churn
 