FROM flowable/java8_server

ADD target/flowable-rest.war flowable-rest.war
ADD src/main/docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/flowable-rest.war"]