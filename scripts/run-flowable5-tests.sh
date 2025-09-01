#!/bin/sh
echo "Building dependencies"
cd ..
./mvnw clean install -DskipTests -Pflowable5-test $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error building dependencies. Exiting."
  exit $STATUS
fi

./mvnw test -Pflowable5-test \
  -pl modules/flowable5-compatibility,modules/flowable5-test,modules/flowable5-spring,modules/flowable5-spring-compatibility,modules/flowable5-spring-test,modules/flowable5-cxf-test,modules/flowable5-camel-test \
  $MAVEN_CONFIG

cd ../..

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while testing Flowable 5. Exiting."
  exit $STATUS
fi


echo "All good!"
