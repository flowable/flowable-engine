#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn -Pdocker,swagger clean deploy

echo "Building and pushing UI app image"
cd ../flowable-ui
mvn -Pdocker clean deploy

echo "Done..."