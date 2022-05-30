FROM eclipse-temurin:11.0.15_10-jdk-alpine@sha256:37c5e98456f7a9a36d4e385a2d068963b82c1c8e2d4943610d93d13c5f65324a

LABEL maintainer="Flowable <info@flowable.org>"
RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*

RUN addgroup -S flowable && adduser -S flowable -G flowable

RUN mkdir /data && chown flowable:flowable /data && \
    chgrp -R 0 /data && \
    chmod -R g=u /data

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh

USER flowable:flowable