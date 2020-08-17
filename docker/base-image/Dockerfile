FROM adoptopenjdk/openjdk11:jre-11.0.7_10-alpine

MAINTAINER Flowable <info@flowable.org>
RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh