FROM flowable/java8_server

ADD target/flowable-admin.war flowable-admin.war
ADD docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/flowable-admin.war"]