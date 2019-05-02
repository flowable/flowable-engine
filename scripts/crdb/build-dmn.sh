#!/bin/bash
   
./common.sh

cd ../..
mvn -Ddatabasedmn=cockroachdb clean install