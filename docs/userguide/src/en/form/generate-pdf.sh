#!/bin/bash
rm -rf output/flowable-form-userguide.pdf
asciidoctor-pdf  -o output/flowable-form-userguide.pdf index-pdf.adoc
