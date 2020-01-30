FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*
ARG WAR_FILE
ADD ${WAR_FILE} app.war
ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh
ENTRYPOINT ["java", "-jar", "/app.war"]