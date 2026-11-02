#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building REST app image"
cd modules/flowable-app-rest
mvn -Pdocker,swagger -DskipTests clean package

echo "Done..."
