#!/bin/bash
cd ..

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn clean package -PdockerPublish,swagger

echo "Building and pushing UI app image"
cd ../flowable-ui
mvn clean package -pl flowable-ui-app -PdockerPublish -DskipTests

echo "Done..."