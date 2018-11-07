#!/bin/sh
mvn -T 1C clean install -DskipTests
STATUS=$?
if [ $STATUS -eq 0 ]
then
	cd flowable-ui-idm-app

	# Run war
	export MAVEN_OPTS="$MAVEN_OPTS -noverify -Xms512m -Xmx1024m -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
	mvn clean install -Pmysql -DskipTests spring-boot:run
else
    echo "Error while building root pom. Halting."
fi
