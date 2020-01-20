# docker-compose-hazelcast-jet makefile

# Environment Variables
CONTAINER_SERVER = hazelcast-jet
CONTAINER_CLIENT = hazelcast-jet-submit
CONTAINER_MYSQL = mysql
CONTAINER_KAFKA = kafka
CONTAINER_KAFKA_CONNECT = kafka-connect

export JOB_JAR= /jars/$(shell ls -tr jars| tail -2)

.PHONY: up

prep :
	echo  $(JOB_JAR) file will be submitted to the cluster....

pull :
	docker-compose -f cdc.yml pull

up : prep pull
	docker-compose -f cdc.yml up -d

down :
	docker-compose -f cdc.yml down

restart :
	docker-compose -f cdc.yml restart $(CONTAINER_CLIENT)  

connectDb :
	docker-compose -f cdc.yml exec $(CONTAINER_MYSQL) mysql -umysqluser -pmysqlpw  inventory

startDebezium :
	curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @mysql-connector.json

tailDb :
	docker-compose -f cdc.yml logs -f $(CONTAINER_MYSQL)

tailServer :
	docker-compose -f cdc.yml logs -f $(CONTAINER_SERVER)

tailClient :
	docker-compose -f cdc.yml logs -f $(CONTAINER_CLIENT)

tailKafkaConnect :
	docker-compose -f cdc.yml logs -f $(CONTAINER_KAFKA_CONNECT)

tailKafka :
	docker-compose -f cdc.yml logs -f $(CONTAINER_KAFKA)

tailKafkaCustomersTopic :
	docker-compose -f cdc.yml exec $(CONTAINER_KAFKA) sh -c "export KAFKA_BROKER=kafka:9092; /docker-entrypoint.sh watch-topic -a dbserver1.inventory.customers"


