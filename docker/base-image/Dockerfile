FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

LABEL maintainer="Flowable <info@flowable.org>"
RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*

RUN addgroup -S flowable && adduser -S flowable -G flowable

RUN mkdir /data && chown flowable:flowable /data && \
    chgrp -R 0 /data && \
    chmod -R g=u /data

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh

USER flowable:flowable