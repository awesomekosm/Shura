#!/bin/bash

./mvnw clean package

docker build --tag local/shura:latest .

docker rm -f shura || true

docker run -d \
	--name shura \
	-v $(pwd)/cache:/opt/cache \
  --env JAVA_OPTS="-Dshura.discord.token=$1" \
	local/shura:latest

docker logs -f shura
