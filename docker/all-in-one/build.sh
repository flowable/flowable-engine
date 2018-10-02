#!/bin/sh
basePwd="$PWD"

echo "Initializing"

rm $basePwd/assets/*.original

echo "Building Flowable IDM"
cd ../../modules/flowable-ui-idm
mvn -T 1C clean install -DskipTests -Pdocker-deps
STATUS=$?
if [ $STATUS -eq 0 ]
then
    echo "Copying artifact"
	cp flowable-ui-idm-app/target/*.original $basePwd/assets/
else
    echo "Error while building root pom. Halting."
fi

cd $basePwd

echo "Building Flowable Modeler"
cd ../../modules/flowable-ui-modeler
mvn -T 1C clean install -DskipTests -Pdocker-deps
STATUS=$?
if [ $STATUS -eq 0 ]
then
    echo "Copying artifact"
	cp flowable-ui-modeler-app/target/*.original $basePwd/assets/
else
    echo "Error while building root pom. Halting."
fi

cd $basePwd

echo "Building Flowable Task"
cd ../../modules/flowable-ui-task
mvn -T 1C clean install -DskipTests -Pdocker-deps
STATUS=$?
if [ $STATUS -eq 0 ]
then
    echo "Copying artifact"
	cp flowable-ui-task-app/target/*.original $basePwd/assets/
else
    echo "Error while building root pom. Halting."
fi

cd $basePwd

echo "Building Flowable Admin"
cd ../../modules/flowable-ui-admin
mvn -T 1C clean install -DskipTests -Pdocker-deps
STATUS=$?
if [ $STATUS -eq 0 ]
then
    echo "Copying artifact"
	cp flowable-ui-admin-app/target/*.original $basePwd/assets/
else
    echo "Error while building root pom. Halting."
fi

cd $basePwd

echo "Building Docker image"

docker build -t flowable/all-in-one:latest -t flowable/all-in-one:6.4.0 .
