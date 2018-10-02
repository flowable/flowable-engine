FROM flowable/java8_server:8.181
ARG WAR_FILE
ADD ${WAR_FILE} app.war
ADD wait-for-something.sh .
RUN chmod +x wait-for-something.sh
ENTRYPOINT ["java", "-jar", "/app.war"]