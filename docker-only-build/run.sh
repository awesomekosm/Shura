#!/bin/bash

docker rmi local/shura

docker build --pull --no-cache --tag local/shura:latest .

docker rm -f shura || true

docker run -d \
  --name shura \
  --env JAVA_OPTS="-Dshura.discord.token=$1" \
  local/shura:latest

docker logs -f shura
