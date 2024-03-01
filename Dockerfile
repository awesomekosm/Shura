# https://hub.docker.com/_/eclipse-temurin/tags?page=1&name=21-jre-alpine
FROM eclipse-temurin:21-jre-alpine

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk --no-cache add ffmpeg python3 ca-certificates && \
    ln -sf python3 /usr/bin/python && \
    mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./youtube-dl && \
    chmod a+rx ./youtube-dl && \
    mkdir /opt/cache

ADD target/shura-*.jar /opt/java/app.jar

WORKDIR /opt

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/java/app.jar
