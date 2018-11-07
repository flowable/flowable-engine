#!/bin/bash

if [ -z "$1" ]
then
  echo -e "Usage: \n${0##*/} all \n${0##*/} bpmn \n${0##*/} cmmn \n${0##*/} dmn \n${0##*/} form"
  exit 1
fi

if [ $1 == all ]
then
  rm -rf ../src/en/single/output
  rm -rf ../src/en/bpmn/output
  rm -rf ../src/en/cmmn/output
  rm -rf ../src/en/dmn/output
  rm -rf ../src/en/form/output
else
   rm -rf ../src/en/$1/output
fi
