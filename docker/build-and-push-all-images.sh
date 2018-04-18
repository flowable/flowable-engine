#!/bin/bash
echo "Building all Java artifacts"
cd ..
mvn -Pdistro clean install -DskipTests

echo "Building and pushing REST app image"
cd modules/flowable-app-rest
mvn -Pdocker,swagger clean deploy

echo "Building and pushing ADMIN app image"
cd ../flowable-ui-admin
mvn -Pdocker clean deploy

echo "Building and pushing IDM app image"
cd ../flowable-ui-idm
mvn -Pdocker clean deploy

echo "Building and pushing MODELER app image"
cd ../flowable-ui-modeler
mvn -Pdocker clean deploy

echo "Building and pushing TASK app image"
cd ../flowable-ui-task
mvn -Pdocker clean deploy

echo "Done..."