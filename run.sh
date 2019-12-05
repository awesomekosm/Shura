#!/bin/bash

./gradlew clean \

./gradlew bootJar \

docker build --tag local/shura:latest . \

docker stop shura \

docker rm shura \

docker run --name shura --env JAVA_OPTS="-Ddiscord.token=$1" local/shura
