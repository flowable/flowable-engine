#!/bin/bash
   
./common.sh

cd ../..
mvn -Ddatabase=cockroachdb clean install