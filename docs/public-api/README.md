# Flowable Rest API Documentation

This project contains all informations & tooling about Flowable Public Rest API.

## Introduction

Flowable includes a set of Public REST API to the Flowable Engine.

We distinct currently 4 groups of APIs 
+ **Process API** : Provides access to most of the Flowable services
+ **Decision API** : Provides access to the Flowable DMN engine
+ **Form API** : Provides access to the Flowable Form engine
+ **Content API** : Provides access to the Flowable Content Services
 
### Quick Installation
 
Flowable REST API can be installed by deploying the **flowable-rest.war** or **flowable-task.war**  file to a servlet container like Apache Tomcat.

It's also possible to use **Docker** images to easily boot up an environment.
```bash 
docker run -p8080:8080 flowable/flowable-rest
```

### Quick Integration

The Flowable REST API can also be used in another web-application by including the servlet and its mapping in your application and add all flowable-rest dependencies to the classpath.

### Contribution & Documentation Issues

If you spot a documentation issue please raise an issue directly on [Github](https://github.com/flowable/flowable-engine/issues) or create a [Pull Request](https://github.com/flowable/flowable-engine/pulls)

# References

**[OpenAPI](https://github.com/OAI/OpenAPI-Specification)** is a standard specification to describe a REST API. It's vendor neutral and backed by the [OpenAPI initiative](https://www.openapis.org/) which includes [many software companies](https://www.openapis.org/membership/members) (Google, Microsoft, IBM, Atlassian...). 

## Swagger Specification V2

You can retrieve the Swagger Specification for Flowable API by following links below.

| API Name | Syntax Validation  | 
|:---:|:---:|
| [Process API](/references/swagger/process/flowable.yaml) | <img src="http://online.swagger.io/validator?url=https://raw.githubusercontent.com/flowable/flowable-engine/master/docs/public-api/references/swagger/process/flowable-swagger-process.yaml">  |  
| [Form API](/references/swagger/form/flowable.yaml) |  <img src="http://online.swagger.io/validator?url=https://raw.githubusercontent.com/flowable/flowable-engine/master/docs/public-api/references/swagger/form/flowable-swagger-form.yaml"> |  
| [Decision API](/references/swagger/decision/flowable.yaml) |  <img src="http://online.swagger.io/validator?url=https://raw.githubusercontent.com/flowable/flowable-engine/master/docs/public-api/references/swagger/decision/flowable-swagger-decision.yaml"> | 
| [Content API](/references/swagger/content/flowable.yaml) | <img src="http://online.swagger.io/validator?url=https://raw.githubusercontent.com/flowable/flowable-engine/master/docs/public-api/references/swagger/content/flowable-swagger-content.yaml">  | 


## Open API Specification V3

You can retrieve the OpenApi Specification (V3) for Flowable API by following links below.


| API Name |
|:---:|
| [Process API](/references/oas/process/flowable-swagger-process.yaml) |    
| [Form API](/references/oas/form/flowable-swagger-form.yaml) | 
| [Decision API](/references/oas/decision/flowable-swagger-decision.yaml) |  
| [Content API](/references/oas/content/flowable-swagger-content.yaml) |   

# Tools

To help developers discovering & using Flowable API, we provide a set of tools ready to use.

## [Flowable OpenAPI Specification Generator](/tools/flowable-oas-generator)

Utility project to generate automatically the swagger definition from the source code.

## [Flowable Rest Asciidoc](/tools/flowable-rest-asciidoc)

Utility project to generate asciidoc based on OAS files.

## [Flowable Swagger UI](/tools/flowable-swagger-ui)

Utility project to build a WAR file for Swagger UI.

## [Flowable Slate](/tools/flowable-slate)

Utility project to create a beautiful static web site for REST API docs.
