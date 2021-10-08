# https://hub.docker.com/r/adoptopenjdk/openjdk11/tags?page=1&ordering=last_updated&name=x86_64-alpine-jre-11
FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.12_7

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk add ffmpeg python3 && \
    ln -sf python3 /usr/bin/python && \
    mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./youtube-dl && \
    chmod a+rx ./youtube-dl && \
    mkdir /opt/cache

ADD target/shura-*.jar /opt/app.jar

WORKDIR /opt

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
