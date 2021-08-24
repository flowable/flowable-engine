#!/bin/sh
JAVA_OPTS="${JAVA_OPTS}
  -Djava.awt.headless=true
  -Dfile.encoding=UTF-8"

cmd="java ${JAVA_OPTS} \
          -cp /app:/app/WEB-INF/classes:/app/WEB-INF/lib/* \
          org.flowable.ui.application.FlowableUiApplication \
          ${@}"

exec $cmd