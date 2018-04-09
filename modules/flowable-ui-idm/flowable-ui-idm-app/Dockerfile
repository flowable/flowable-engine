FROM flowable/java8_server

ADD target/flowable-idm.war flowable-idm.war
ADD src/main/docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/flowable-idm.war"]