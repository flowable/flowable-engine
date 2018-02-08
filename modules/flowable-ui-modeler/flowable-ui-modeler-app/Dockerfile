FROM java:8
MAINTAINER Simon Yang <simon.yang@relational.com.au>

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /designer.jar

ADD waitForOracle /waitForOracle/
RUN chmod +x /waitForOracle/wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/designer.jar", "-httpPort=8888", "-httpProtocol=org.apache.coyote.http11.Http11NioProtocol"]

