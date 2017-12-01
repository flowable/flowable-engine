#!/bin/bash
rm -rf output/flowable-cmmn-userguide.pdf
asciidoctor-pdf  -o output/flowable-cmmn-userguide.pdf index-pdf.adoc
