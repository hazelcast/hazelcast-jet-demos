package com.rlab.hazelcast.ServerMember;

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
    public static void main(String[] args) {
    	Config config;
		try {
			config = new FileSystemXmlConfig("src/main/resources/hazelcast.xml");
			HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
			hz.getClientService().addClientListener(new ClientListener(){

				public void clientConnected(Client arg0) {
					// TODO Auto-generated method stub
					
				}

				public void clientDisconnected(Client arg0) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			HazelcastClient.newHazelcastClient().getClientService().addClientListener(new ClientListener(){

				public void clientConnected(Client arg0) {
					// TODO Auto-generated method stub
					
				}

				public void clientDisconnected(Client arg0) {
					// TODO Auto-generated method stub
					
				}
				
			});
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
}

//vm options -Xms4G -Xmx4G -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+UseG1GC -Dhazelcast.partition.count=271