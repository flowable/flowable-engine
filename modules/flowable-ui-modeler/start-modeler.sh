#!/bin/sh
echo "Building all modeler ui submodules"
mvn -T 1C clean install -DskipTests
STATUS=$?
if [ $STATUS -eq 0 ]
then
	cd flowable-ui-modeler-app

	# Run war
	echo "Running war file"sd
	export MAVEN_OPTS="$MAVEN_OPTS -noverify -Xms512m -Xmx1024m -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n"
	mvn clean tomcat7:run -Pmysql -Ddeployment.api.url=http://localhost:9999/flowable-task/process-api
else
    echo "Error while building root pom. Halting."
fi
	
