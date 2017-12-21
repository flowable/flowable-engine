# Flowable OpenAPI Specification Generator

## Introduction

Utility project to generate automatically the swagger definition from the source code.

## Usage

Build for flowable-rest in localhost
```bash 
mvn clean package
```

Build for flowable-task in localhost
```bash 
mvn clean package -Ptask-app
```

Build for flowable-task with a specific hostname
```bash 
mvn clean package -Ptask-app -Dswagger.host=10.0.0.1:9090
```