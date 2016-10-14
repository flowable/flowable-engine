#!/bin/bash

asciidoctor -a stylesheet=flowable.css -o output/index.html index-html.adoc
asciidoctor -a stylesheet=flowable.css -o output/migration.html migration.adoc

rm -rf output/images
mkdir output/images
cp -r images output
