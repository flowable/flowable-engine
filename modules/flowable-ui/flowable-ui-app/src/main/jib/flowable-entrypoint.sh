#!/bin/sh
JAVA_OPTS="${JAVA_OPTS}
  -Djava.awt.headless=true
  -Dfile.encoding=UTF-8"

cmd="java ${JAVA_OPTS} \
          -cp /app:/app/WEB-INF/classes:/app/WEB-INF/lib/* \
          org.flowable.ui.application.FlowableUiApplication \
          ${@}"

## if user is part of root group, then 
if [ $(id -g) -eq 0 -a $(id -u) -ne 0 ]
then
        exec $cmd
else
        exec su-exec flowable:flowable $cmd
fi