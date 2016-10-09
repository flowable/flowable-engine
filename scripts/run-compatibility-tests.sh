#!/bin/sh
echo "Dropping DB schema"
mysql -u alfresco -palfresco -e "DROP SCHEMA activiticompatibility"

echo "Creating DB schema"
mysql -u alfresco -palfresco -e "CREATE SCHEMA activiticompatibility DEFAULT CHARACTER SET utf8 COLLATE utf8_bin"

echo "Building dependencies" 
cd ..
mvn clean install -DskipTests

cd modules/flowable5-engine/
mvn clean install -DskipTests
cd ../..

cd modules/flowable5-compatibility/
mvn clean install -DskipTests
cd ../..

echo "Building test data generators"
cd modules/flowable5-compatibility-testdata
mvn clean package shade:shade

echo "Generating test data"
cd target
java -jar flowable5-compatibility-testdata.jar

echo "Running tests"
cd ../../flowable5-compatibility-test
mvn clean test
