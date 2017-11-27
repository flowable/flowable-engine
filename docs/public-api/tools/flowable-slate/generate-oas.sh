#!/usr/bin/env bash

# CLEAN
rm -rf target
mkdir -p target/specfile

# COPY SPEC FILES Automatically Generated
# cp -r ../flowable-oas-generator/target/oas/v2/ target/specfile
# COPY SPEC FILES based on References
 cp -r ../../references/openapi/ target/specfile


for apiName in {"content","decision","form","process"}; do
  # EXECUTE WINDERSHIN
  widdershins --summary --noschema  --user_templates templates/ -y target/specfile/$apiName/flowable-oas-$apiName.yaml target/specfile/$apiName/_rest-body.md

  # COPY TO SLATE
  # Remove header from the body (roughly the first 40 lines)
  sed -e '1,40d' target/specfile/$apiName/_rest-body.md > slate/source/includes/_rest-body.md
  # Add Header API Name Title.
  title="$(tr '[:lower:]' '[:upper:]' <<< ${apiName:0:1})${apiName:1}"
  sed -e "s/API_NAME/$title/g" templates/_rest-title.md > slate/source/includes/_rest-title.md

  # BUILD SLATE
  cd slate
  bundle exec middleman build --clean

  # MOVE TO TARGET
  cd ..
  mkdir -p target/slate/$apiName
  mv slate/build/* target/slate/$apiName

done







