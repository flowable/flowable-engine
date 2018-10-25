FROM flowable/java8_server:8.181
ADD wait-for-something.sh .

RUN addgroup tomcat && adduser -s /bin/false -G tomcat -h /opt/tomcat -D tomcat

RUN wget http://www-eu.apache.org/dist/tomcat/tomcat-8/v8.5.34/bin/apache-tomcat-8.5.34.tar.gz -O /tmp/tomcat.tar.gz
RUN cd /tmp && tar xvfz tomcat.tar.gz && cp -Rv /tmp/apache-tomcat-8.5.34/* /opt/tomcat/ && rm -Rf /tmp/apache-tomcat-8.5.34

COPY assets/flowable-idm.war.original /opt/tomcat/webapps/flowable-idm.war
COPY assets/flowable-modeler.war.original /opt/tomcat/webapps/flowable-modeler.war
COPY assets/flowable-task.war.original /opt/tomcat/webapps/flowable-task.war
COPY assets/flowable-admin.war.original /opt/tomcat/webapps/flowable-admin.war

RUN cd /opt/tomcat && chgrp -R tomcat /opt/tomcat && chmod -R g+r conf && chmod g+x conf && chown -R tomcat webapps/ work/ temp/ logs/ \
    && chown tomcat /wait-for-something.sh && chmod +x /wait-for-something.sh

ENV CATALINA_HOME /opt/tomcat
ENV PATH $PATH:$CATALINA_HOME/bin
ENV JAVA_OPTS="-Xms1024M -Xmx1024M -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

WORKDIR /opt/tomcat

USER tomcat

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
