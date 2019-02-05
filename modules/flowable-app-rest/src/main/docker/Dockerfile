FROM adoptopenjdk/openjdk8:alpine-slim
ARG WAR_FILE
ADD ${WAR_FILE} app.war
ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh
ENTRYPOINT ["java", "-jar", "/app.war"]