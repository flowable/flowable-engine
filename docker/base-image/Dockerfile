FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

MAINTAINER Flowable <info@flowable.org>
RUN apk add --no-cache ttf-dejavu su-exec && rm -rf /var/cache/apk/*

RUN addgroup -S flowable && adduser -S flowable -G flowable

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh