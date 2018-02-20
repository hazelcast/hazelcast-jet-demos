package com.rlab.imdg.member;

import java.io.File;
import java.io.FileNotFoundException;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Client;
import com.hazelcast.core.ClientListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
public class ServerMember {
	
	static HazelcastInstance hz =null;
    public static void main(String[] args) {
		hazelcastInstance();
    }
    
   public  static HazelcastInstance hazelcastInstance(){
	   Config config;
		try {
			if(hz ==null){
			config = new FileSystemXmlConfig("src/main/resources/hazelcast.xml");
			 hz = Hazelcast.newHazelcastInstance(config);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return hz;
    }
}

//vm options -Xms4G -Xmx4G -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+UseG1GC -Dhazelcast.partition.count=271