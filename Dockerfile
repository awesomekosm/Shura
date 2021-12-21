# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=17.0.1-17.30.15-jre-headless
FROM azul/zulu-openjdk-debian:17.0.1-17.30.15-jre-headless@sha256:b28aaf77069a184dbbfdc82043bc13ab4f69f54bc351e677bc371b4a139568ab

ARG JAVA_OPTS

RUN apt-get update && apt-get install -y \
    ffmpeg python3 wget && \
    rm -rf /var/lib/apt/lists/* && \
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
