#!/usr/bin/env bash

# CLEAN
rm -rf target
mkdir -p target/specfile

# COPY SPEC FILES Automatically Generated
# cp -r ../flowable-oas-generator/target/oas/v2/ target/specfile
# COPY SPEC FILES based on References
 cp -r ../../references/swagger/ target/specfile


for apiName in {"content","decision","form","process"}; do
  # EXECUTE WINDERSHIN
  widdershins --summary -y target/specfile/$apiName/flowable-swagger-$apiName.yaml target/specfile/$apiName/index.html.md

  # COPY TO SLATE
  cp target/specfile/$apiName/index.html.md slate/source

  # BUILD SLATE
  cd slate
  bundle exec middleman build --clean

  # MOVE TO TARGET
  cd ..
  mkdir -p target/slate/$apiName
  mv slate/build/* target/slate/$apiName

done







