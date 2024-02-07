#!/usr/bin/env bash
rm -rf target
mvn -DskipTests -Pnative native:compile
./target/flowable-spring-boot-sample-native
