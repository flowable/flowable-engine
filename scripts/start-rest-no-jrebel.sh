#!/bin/bash
export MAVEN_OPTS="-Xms521M -Xmx1024M -XX:MaxPermSize=256M -noverify -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n"
cd ..
mvn -T 1C -PbuildRestappDependencies clean install
STATUS=$?
if [ $STATUS -eq 0 ]
then
    cd modules/flowable-app-rest
    mvn -Dfile.encoding=UTF-8 -Dswagger.host=localhost:8080 -Pmysql clean package tomcat7:run
else
    echo "Build failure in dependent project. Cannot boot Flowable Rest."
fi
