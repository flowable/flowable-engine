FROM flowable/java8_server
MAINTAINER Simon Yang <simon.yang@relational.com.au>

# Add the service itself
ARG JAR_FILE
ADD target/rds-idm.jar /rds-idm.jar
ADD src/main/docker/wait-for-something.sh /wait-for-something.sh
RUN chmod +x /wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/rds-idm.jar", "-httpPort=8888", "-httpProtocol=org.apache.coyote.http11.Http11NioProtocol"]

