# Flowable Docker base image

Extends from azul/zulu-openjdk-alpine:21-jre-latest

Adds `flowable:flowable` user which can be used to 'step down' from root when executing Flowable applications.

#### modifications

* `RUN apk add --no-cache ttf-dejavu su-exec && rm -rf /var/cache/apk/*`
* `RUN addgroup -S flowable && adduser -S flowable -G flowable`
* `ADD wait-for-something.sh .`
* `RUN chmod +x wait-for-something.sh`
