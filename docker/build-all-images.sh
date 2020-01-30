#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building REST app image"
cd modules/flowable-app-rest
mvn -Pdocker,swagger -DskipTests clean package

echo "Building ADMIN app image"
cd ../flowable-ui-admin
mvn -Pdocker -DskipTests clean package

echo "Building IDM app image"
cd ../flowable-ui-idm
mvn -Pdocker -DskipTests clean package

echo "Building MODELER app image"
cd ../flowable-ui-modeler
mvn -Pdocker -DskipTests clean package

echo "Building TASK app image"
cd ../flowable-ui-task
mvn -Pdocker -DskipTests clean package

echo "Done..."
