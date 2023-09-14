# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=19-latest
FROM azul/zulu-openjdk-debian:19-latest AS builder

ARG JAVA_OPTS

RUN apt-get update && apt-get install -y \
    git && \
    rm -rf /var/lib/apt/lists/* && \
    git --version && \
    cd /opt && \
    git clone https://github.com/awesomekosm/Shura.git && \
    cd Shura && \
    ./mvnw package && \
    cp /opt/Shura/target/shura-*.jar /opt/app.jar && \
    rm -rf /opt/shura-source

# https://hub.docker.com/r/azul/zulu-openjdk-debian/tags?page=1&name=19-jre-headless-latest
FROM azul/zulu-openjdk-debian:19-jre-headless-latest

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
