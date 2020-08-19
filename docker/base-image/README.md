# Flowable Docker base image

Extends from adoptopenjdk/openjdk11:jre-11.0.7_10-alpine

#### modifications

* `RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*`
* `ADD wait-for-something.sh .`
* `RUN chmod +x wait-for-something.sh`
