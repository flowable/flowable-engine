FROM eclipse-temurin:11.0.15_10-jre-alpine@sha256:d401c4025eb1ead95cf2ca83cd5155bff8756527a4f4e9820570900e835f5ba4

LABEL maintainer="Flowable <info@flowable.org>"
RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*

RUN addgroup -S flowable && adduser -S flowable -G flowable

RUN mkdir /data && chown flowable:flowable /data && \
    chgrp -R 0 /data && \
    chmod -R g=u /data

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh

USER flowable:flowable