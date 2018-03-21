#!/bin/bash
export MAVEN_OPTS="-Xms1024m -Xmx2048m -noverify -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8005,server=y,suspend=n"
cd ..
mvn -T 1C -PbuildRestappDependencies clean install
STATUS=$?
if [ $STATUS -eq 0 ]
then
    cd modules/flowable-app-rest
    mvn -Dfile.encoding=UTF-8 -Dswagger.host=localhost:8080 -Pswagger,mysql clean package tomcat7:run
else
    echo "Build failure in dependent project. Cannot boot Flowable Rest."
fi
