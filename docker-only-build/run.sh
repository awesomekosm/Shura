#!/bin/bash

docker build --tag local/shura:latest .

docker rm -f shura || true

docker run -d --name shura --env JAVA_OPTS="-Dshura.discord.token=$1" local/shura
