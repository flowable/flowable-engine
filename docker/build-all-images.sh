#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building REST app image"
cd modules/flowable-app-rest
mvn -Pdocker,swagger -DskipTests clean package

echo "Building UI app image"
cd ../flowable-ui
mvn -Pdocker -DskipTests clean package

echo "Done..."
