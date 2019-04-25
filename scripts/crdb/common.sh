#!/bin/bash
   
FILE=$HOME/.flowable/jdbc/build.flowable6.cockroachdb.properties
if [ -f $FILE ]; then
   rm $FILE
fi

touch $FILE
echo "jdbc.url=jdbc:postgresql://127.0.0.1:26257/flowable?sslmode=disable" >> $FILE
echo "jdbc.driver=org.postgresql.Driver" >> $FILE
echo "jdbc.username=flowable" >> $FILE
echo "jdbc.password" >> $FILE