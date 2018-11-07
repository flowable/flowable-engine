#!/bin/bash
rm -rf output/flowable-dmn-userguide.pdf
asciidoctor-pdf  -o output/flowable-dmn-userguide.pdf index-pdf.adoc
