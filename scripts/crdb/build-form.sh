#!/bin/bash
   
./common.sh

cd ../..
mvn -Ddatabaseform=cockroachdb clean install