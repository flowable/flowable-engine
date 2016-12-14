#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building REST app image"
cd modules/flowable-app-rest
mvn -PbuildDockerImage,swagger clean package

echo "Building ADMIN app image"
cd ../flowable-ui-admin
mvn -PbuildDockerImage clean package

echo "Building IDM app image"
cd ../flowable-ui-idm
mvn -PbuildDockerImage clean package

echo "Building MODELER app image"
cd ../flowable-ui-modeler
mvn -PbuildDockerImage clean package

echo "Building TASK app image"
cd ../flowable-ui-task
mvn -PbuildDockerImage clean package

echo "Done..."
