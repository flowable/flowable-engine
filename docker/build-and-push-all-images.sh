#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn clean install -Pdistro -DskipTests -T 2C

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn clean package -PdockerPublish,swagger -DskipTests 

echo "Building and pushing UI app image"
cd ../flowable-ui
mvn clean package -PdockerPublish -DskipTests

echo "Done..."