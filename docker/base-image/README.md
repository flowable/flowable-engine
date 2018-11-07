# Flowable Docker base image

This is a fork of [Anapsix Docker Alpine Java image](https://github.com/anapsix/docker-alpine-java) containing some modifications.

#### modifications

* Alpine version 3.8.1; *which includes a fix to prevent potential remote execution*
* Include Nashorn (Javascript engine); *is not included in the original Anapsix images*

#### used versions
* Java SE 8u181 (server-jre)
* Java JCE unlimited
* Alpine 3.8.1
* GLIBC 2.28
