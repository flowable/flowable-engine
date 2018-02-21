#!/bin/sh
mvn -T 1C clean install -DskipTests
STATUS=$?
if [ $STATUS -eq 0 ]
then
	cd flowable-ui-idm-app

	# Run war
	echo "Running war file"
	export MAVEN_OPTS="$MAVEN_OPTS -noverify -Xms512m -Xmx1024m -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
	mvn clean tomcat7:run -Pmysql
else
	say -v Cellos "Dum dum dum dum dum dum dum he he he ho ho ho fa lah lah lah lah lah lah fa lah full hoo hoo hoo"
    echo "Error while building root pom. Halting."
fi
