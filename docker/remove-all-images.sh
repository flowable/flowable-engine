#!/bin/bash
echo "Removing all 'flowable' images"
docker rmi $(docker images |grep flowable|awk '{print $3;}')
