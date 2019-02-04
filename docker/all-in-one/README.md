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
[adoptopenjdk/openjdk8:alpine-slim](https://github.com/AdoptOpenJDK/openjdk-docker/tree/master/8/jdk/alpine)

### Tomcat

version 8.5.34

### Database

in memory h2

### Default user

user: admin  
password: test