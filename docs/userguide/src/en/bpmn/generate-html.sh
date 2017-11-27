#!/bin/bash

asciidoctor -a stylesheet=../base/flowable.css -o output/index.html index-html.adoc
asciidoctor -a stylesheet=../base/flowable.css -o output/migration.html migration.adoc

rm -rf output/images
mkdir output/images
cp -r images output

## Copy Base Images
cp -r ../base/images output/