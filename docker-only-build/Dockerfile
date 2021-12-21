# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=17.0.1-17.30.15
FROM azul/zulu-openjdk-debian:17.0.1-17.30.15@sha256:5ced0dd472bf018830424b1661738eeca49a8a26240fef2cc69f00ad77257199 AS builder

ARG JAVA_OPTS

RUN apt-get update && apt-get install -y \
    git && \
    rm -rf /var/lib/apt/lists/* && \
    git --version && \
    cd /opt && \
    git clone https://github.com/Z-EMB/Shura.git && \
    cd Shura && \
    ./mvnw package && \
    cp /opt/Shura/target/shura-*.jar /opt/app.jar && \
    rm -rf /opt/shura-source

# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=17.0.1-17.30.15-jre-headless
FROM azul/zulu-openjdk-debian:17.0.1-17.30.15-jre-headless@sha256:b28aaf77069a184dbbfdc82043bc13ab4f69f54bc351e677bc371b4a139568ab

RUN apt-get update && apt-get install -y \
    ffmpeg python3 wget && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf python3 /usr/bin/python

COPY --from=builder /opt/app.jar /opt/app.jar

WORKDIR /opt

RUN mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./youtube-dl && \
    chmod a+rx ./youtube-dl && \
    mkdir /opt/cache

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
