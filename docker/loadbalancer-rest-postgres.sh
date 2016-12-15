#!/bin/bash
DOCKER_COMPOSE_FILE="config/loadbalancer-rest-postgres.yml"

if [ -z "$1" ]
then
  echo -e "Usage: \n${0##*/} start \n${0##*/} stop \n${0##*/} scale [number-of-instances] \n${0##*/} info \n\nHAProxy statistics on http://localhost:8081  (flowable/flowable)"
  exit 1
fi

if [ $1 == start ]
then
  docker-compose -f $DOCKER_COMPOSE_FILE up -d
  STATUS=$?
  if [ $STATUS -eq 0 ]
  then
    echo -e "\nContainers starting in background \nFor log info: \n${0##*/} info"
  else
    echo -e "\nFailed starting containers"
  fi
elif [ $1 == stop ]
then
  docker-compose -f $DOCKER_COMPOSE_FILE down
  STATUS=$?
  if [ $STATUS -eq 0 ]
  then
    echo -e "\nContainers successfully stopped"
  else
    echo -e "\nFailed stopping containers"
  fi
elif [ $1 == info ]
then
  docker-compose -f $DOCKER_COMPOSE_FILE logs --follow
elif [ $1 == scale ]
then
  if [ -z "$2" ]
  then
    echo -e "Number of total instances missing; f.e.: \n${0##*/} scale 2"
    exit 1
  else
    docker-compose -f $DOCKER_COMPOSE_FILE scale flowable-rest-app=$2
    STATUS=$?
    if [ $STATUS -eq 0 ]
    then
      echo -e "\nServices scaling out in background \nFor log info: \n${0##*/} info"
    else
      echo -e "\nFailed scaling out services"
    fi
  fi
else
  echo -e "Usage: \n${0##*/} start \n${0##*/} stop \n${0##*/} info"
  exit 1
fi
