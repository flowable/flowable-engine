#!/bin/bash
rm -rf output/activiti-userguide.pdf
asciidoctor-pdf -r asciidoctor-pdf-cjk-kai_gen_gothic -a pdf-style=KaiGenGothicCN  -o output/activiti-userguide.pdf index-pdf.adoc
