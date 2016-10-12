#!/bin/bash
rm -rf output/activiti-userguide.pdf
asciidoctor-pdf  -o output/activiti-userguide.pdf index-pdf.adoc
