#!/bin/sh
echo "Building dependencies"
cd ..
mvn clean install -DskipTests -Pcheck $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error building v6 dependencies. Exiting."
  exit $STATUS
fi

cd modules/flowable5-engine/
mvn clean install -DskipTests $MAVEN_CONFIG
STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error building v5 dependencies. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-compatibility/
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-compatibility. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-test
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-test. Exiting."
  exit $STATUS
else
  echo "All Flowable 5 tests succeeded"
fi

cd ../..

cd modules/flowable5-spring
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-spring. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-spring-compatibility/
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-spring-compatibility. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-spring-test/
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-spring-test. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable-cxf/
mvn clean install -DskipTests $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable-cxf. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-cxf-test/
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-cxf-test. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable-camel/
mvn clean install -DskipTests $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-camel. Exiting."
  exit $STATUS
fi

cd ../..

cd modules/flowable5-camel-test/
mvn clean install $MAVEN_CONFIG

STATUS=$?
if [ $STATUS -ne 0 ]
then
  echo "Error while building flowable5-camel-test. Exiting."
  exit $STATUS
fi


echo "All good!"
