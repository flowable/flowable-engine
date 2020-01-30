# Flowable All in One image

This image contains all Flowable UI apps running on Tomcat.

```
docker run -p 8080:8080 flowable/all-in-one
```

### Included Flowable apps
* Flowable IDM  (http://localhost:8080/flowable-idm)
* Flowable Modeler  (http://localhost:8080/flowable-modeler)
* Flowable Task  (http://localhost:8080/flowable-task)
* Flowable Admin  (http://localhost:8080/flowable-admin)

### Base image
[adoptopenjdk/openjdk11:alpine-jre](https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/11/jre/alpine/Dockerfile.hotspot.releases.full)

### Tomcat

version 9.0.14

### Database

in memory h2

### Default user

user: admin  
password: test