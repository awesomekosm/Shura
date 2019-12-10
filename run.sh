#!/bin/bash

./gradlew --no-daemon clean

./gradlew --no-daemon bootJar

docker build --tag local/shura:latest .

docker stop shura

docker rm shura

docker run -d --name shura --env JAVA_OPTS="-Ddiscord.token=$1" local/shura
