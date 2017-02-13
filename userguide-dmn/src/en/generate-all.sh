#!/bin/bash
./clean.sh
echo "Generating HTML"
./generate-html.sh
echo "Generating PDF"
./generate-pdf.sh
echo "Done"
