#!/bin/bash
   
./common.sh

cd ../..
mvn -Ddatabaseidm=cockroachdb clean install