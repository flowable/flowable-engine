#!/bin/bash
export MAVEN_OPTS="-Xms1024m -Xmx2048m -noverify -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8005,server=y,suspend=n"
cd ..
mvn -T 1C -Pdistro -DskipTests clean install -pl modules/flowable-app-rest -am
STATUS=$?
if [ $STATUS -eq 0 ]
then
    cd modules/flowable-app-rest
    mvn clean install -Pswagger,mysql spring-boot:run
else
    echo "Build failure in dependent project. Cannot boot Flowable Rest."
fi
