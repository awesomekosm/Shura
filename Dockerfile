# https://hub.docker.com/_/eclipse-temurin/tags?page=1&name=21-jre-alpine
FROM ibm-semeru-runtimes:open-21-jre-jammy

ARG JAVA_OPTS

RUN apt-get update && apt-get install -y \
    ffmpeg python3 wget ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
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
