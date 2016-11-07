#!/bin/bash
cd ..
mvn -T 1C -Pdistro -DskipTests clean install
STATUS=$?
if [ $STATUS -eq 0 ] 
then
    cd modules/flowable-ui-task
    ./start.sh
else
    echo "Build failure in dependent project. Cannot boot Flowable UI."
fi    