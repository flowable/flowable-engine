FROM flowable/java8_server

ADD target/flowable-task.war flowable-task.war
ADD docker/wait-for-something.sh .
RUN chmod +x wait-for-something.sh

ENTRYPOINT ["java", "-jar", "/flowable-task.war"]