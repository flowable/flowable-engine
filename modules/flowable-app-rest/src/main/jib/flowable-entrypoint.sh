#!/bin/sh
JAVA_OPTS="${JAVA_OPTS}
  -Djava.awt.headless=true
  -Dfile.encoding=UTF-8"

exec su-exec flowable:flowable java ${JAVA_OPTS} \
  -cp /app:/app/WEB-INF/classes:/app/WEB-INF/lib/* \
  org.flowable.rest.app.FlowableRestApplication \
  ${@}
