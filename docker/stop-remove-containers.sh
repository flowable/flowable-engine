#!/bin/bash
echo "Stop and remove all 'flowable' containers"
docker rm -f $(docker ps -a |grep flowable|awk '{print $1;}')
