FROM java:8
MAINTAINER Simon Yang <simon.yang@relational.com.au>

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /designer.jar
ADD src/main/docker/wait-for-something.sh /wait-for-something.sh
RUN chmod +x /wait-for-something.sh

RUN apt-get update && apt-get install netcat-openbsd && apt-get clean

ENTRYPOINT ["java", "-jar", "/designer.jar", "-httpPort=8888", "-httpProtocol=org.apache.coyote.http11.Http11NioProtocol"]

