#!/bin/bash

asciidoctor -a stylesheet=../base/flowable.css -o output/index.html index-html.adoc

echo "Retrieving Images"
rm -rf output/images
mkdir output/images

## Copy Images
cp -r ../base/images output/
cp -r ../bpmn/images output/
cp -r ../cmmn/images output/
cp -r ../dmn/images output/
cp -r ../form/images output/

