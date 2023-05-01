FROM eclipse-temurin:11.0.18_10-jdk-alpine@sha256:c79304fa39f7de760f9999697d0a013eca891cab7716d22b78728a179401e2ba

LABEL maintainer="Flowable <info@flowable.org>"
RUN apk add --no-cache ttf-dejavu 'su-exec>=0.2' && rm -rf /var/cache/apk/*
RUN chmod u+s /sbin/su-exec

RUN addgroup -S flowable && adduser -S flowable -G flowable

RUN mkdir /data && chown flowable:flowable /data && \
    chgrp -R 0 /data && \
    chmod -R g=u /data

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh

USER flowable:flowable