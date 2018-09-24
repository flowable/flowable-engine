FROM flowable/java8_server
ARG WAR_FILE
ADD ${WAR_FILE} app.war
ADD src/main/docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh
ENTRYPOINT ["java", "-jar", "/app.war", "-httpPort=8888", "-httpProtocol=org.apache.coyote.http11.Http11NioProtocol"]
