#!/bin/bash
echo -e "Starting Flowable REST APP with loadbalancer \n"
docker-compose -f config/loadbalancer-rest-postgres.yml up
