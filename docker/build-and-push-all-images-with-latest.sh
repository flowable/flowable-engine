#!/bin/bash
cd ..

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn clean package -PdockerPublishWithLatest,swagger

echo "Building and pushing UI app image"
cd ../flowable-ui
mvn clean package -pl flowable-ui-app -PdockerPublishWithLatest

echo "Done..."