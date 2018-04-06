FROM flowable/java8_server

ADD target/flowable-modeler.war flowable-modeler.war
ADD docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/flowable-modeler.war", "-httpPort=8888", "-httpProtocol=org.apache.coyote.http11.Http11NioProtocol"]
