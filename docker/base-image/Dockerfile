FROM azul/zulu-openjdk-alpine:21-jre-latest
LABEL maintainer="Flowable <info@flowable.org>"

RUN apk add --no-cache fontconfig ttf-dejavu && rm -rf /var/cache/apk/*

RUN addgroup -S flowable && adduser -S flowable -G flowable

RUN mkdir /data && chown flowable:flowable /data && \
    chgrp -R 0 /data && \
    chmod -R g=u /data

ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh

USER flowable:flowable