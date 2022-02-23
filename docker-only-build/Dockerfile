# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=17.0.2-17.32.13
FROM azul/zulu-openjdk-debian:17.0.2-17.32.13@sha256:6297de931b679f638aa17e861ccf8c07f845c25a99839aa5ab1730e80b635196 AS builder

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

# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=17.0.2-17.32.13-jre-headless
FROM azul/zulu-openjdk-debian:17.0.2-17.32.13-jre-headless@sha256:1682c3da06c4d2eddb595949a36568540f495e9bf7dbf4c6ac1cc9b660e88878

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
