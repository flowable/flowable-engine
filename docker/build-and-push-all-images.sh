#!/bin/bash
cd ..

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn clean package -PdockerPublish,swagger

echo "Done..."