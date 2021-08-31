#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn -PdockerPublishWithLatest,swagger clean package

echo "Building and pushing UI app image"
cd ../flowable-ui
mvn -PdockerPublishWithLatest clean package

echo "Done..."