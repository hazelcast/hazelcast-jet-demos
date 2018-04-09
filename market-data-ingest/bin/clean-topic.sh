#!/bin/sh

SCRIPT_HOME=`dirname ${0}`

${SCRIPT_HOME}/stop-kafka.sh

COUNT=30
/bin/echo -n Waiting for Kafka shutdown.\n
while [ \( `jps | grep -w Kafka | cut -d" " -f2 | wc -l` -ge 1 \) -a \( $COUNT -gt 0 \) ]
do
  /bin/echo -n .
  COUNT=$[$COUNT - 1]
  sleep 1
done
if [ $COUNT -eq 0 ]
then
 echo Abandoned\n
 exit 1
else
 echo Done\n
fi

${SCRIPT_HOME}/stop-zookeeper.sh
rm -r /tmp/kafka-logs* /tmp/zookeeper

echo ================================
echo Now restart Zookeeper then Kafka
echo ================================

