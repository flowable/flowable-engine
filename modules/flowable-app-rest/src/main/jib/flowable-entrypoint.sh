#!/bin/sh
exec java ${JAVA_OPTS} \
  -cp /app:/app/WEB-INF/classes:/app/WEB-INF/lib/* \
  org.flowable.rest.app.FlowableRestApplication \
  ${@}
