# Flowable Swagger UI

## Introduction

Utility project to build a WAR file for Swagger UI.

## Usage

Run a build
```bash 
mvn clean install
```


## Build

Build the WAR file with Swagger Reference 
```bash 
mvn clean package 
```

Build the WAR file with generated OAS 
```bash 
mvn clean package -Pimport-generator
```