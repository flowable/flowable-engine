FROM adoptopenjdk/openjdk11:alpine-jre
ADD wait-for-something.sh .

RUN apk add --no-cache ttf-dejavu && rm -rf /var/cache/apk/*
RUN addgroup tomcat && adduser -s /bin/false -G tomcat -h /opt/tomcat -D tomcat

RUN wget http://archive.apache.org/dist/tomcat/tomcat-9/v9.0.14/bin/apache-tomcat-9.0.14.tar.gz -O /tmp/tomcat.tar.gz
RUN cd /tmp && tar xvfz tomcat.tar.gz && cp -Rv /tmp/apache-tomcat-9.0.14/* /opt/tomcat/ && rm -Rf /tmp/apache-tomcat-9.0.14

COPY context.xml /opt/tomcat/conf/context.xml

COPY assets/flowable-idm.war.original /opt/tomcat/webapps/flowable-idm.war
COPY assets/flowable-modeler.war.original /opt/tomcat/webapps/flowable-modeler.war
COPY assets/flowable-task.war.original /opt/tomcat/webapps/flowable-task.war
COPY assets/flowable-admin.war.original /opt/tomcat/webapps/flowable-admin.war

RUN cd /opt/tomcat && chgrp -R tomcat /opt/tomcat && chmod -R g+r conf && chmod g+x conf && chown -R tomcat webapps/ work/ temp/ logs/ \
    && chown tomcat /wait-for-something.sh && chmod +x /wait-for-something.sh

ENV CATALINA_HOME /opt/tomcat
ENV PATH $PATH:$CATALINA_HOME/bin
ENV JAVA_OPTS="-Xms512M -Xmx2G -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

WORKDIR /opt/tomcat

USER tomcat

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
