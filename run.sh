#!/bin/bash

./gradlew --no-daemon clean

./gradlew --no-daemon bootJar

docker build --tag local/shura:latest .

docker rm -f shura || true

docker run -d --name shura --env JAVA_OPTS="-Dshura.discord.token=$1" local/shura
