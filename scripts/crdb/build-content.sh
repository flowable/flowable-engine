#!/bin/bash
   
./common.sh

cd ../..
mvn -Ddatabasecontent=cockroachdb clean install