#!/bin/sh
cd `dirname ${0}`

doIt() {
 CMD="$*"
 echo $CMD
 $CMD
 RC=$?
 echo RC=${RC}
}

TMPFILE=/tmp/${0}.$$

doIt ./stop-kafka.sh > $TMPFILE
cat $TMPFILE

COUNT=30
/bin/echo -n Waiting for Kafka shutdown.
while [ \( `jps | grep -w Kafka | cut -d" " -f2 | wc -l` -ge 1 \) -a \( $COUNT -gt 0 \) ]
do
  /bin/echo -n .
  COUNT=$[$COUNT - 1]
  sleep 1
done
if [ $COUNT -eq 0 ]
then
 echo Abandoned
 exit 1
else
 echo Done
fi

doIt ./stop-zookeeper.sh > $TMPFILE
cat $TMPFILE

(cd /tmp; doIt rm -r kafka-logs* zookeeper ) > $TMPFILE
cat $TMPFILE
rm $TMPFILE

echo ================================
echo Now restart Zookeeper then Kafka
echo ================================
