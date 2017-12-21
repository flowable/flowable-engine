# Flowable Rest API - Asciidoc Generator

## Introduction

Utility project to generate asciidoc based on swagger definitions.


## Usage

Execute **mvn install**

Results are in **target/asciidoc** folder


## Integration (manual) with userguide


For a specific API like **process**
+ Go to **target/asciidoc/process**
+ Copy **ch14-REST.adoc**
+ Paste (and overwrite) to **userguide/src/bpmn**
+ Execute html & pdf generation.
