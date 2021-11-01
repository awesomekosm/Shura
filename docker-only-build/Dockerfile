# https://hub.docker.com/r/azul/zulu-openjdk-alpine/tags?page=1&name=17.0.1-17.30.15
FROM azul/zulu-openjdk-alpine:17.0.1-17.30.15 AS builder

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk add git && \
    git --version && \
    cd /opt && \
    git clone https://github.com/Z-EMB/Shura.git && \
    cd Shura && \
    ./mvnw package && \
    cp /opt/Shura/target/shura-*.jar /opt/app.jar && \
    rm -rf /opt/shura-source

# https://hub.docker.com/r/azul/zulu-openjdk-alpine/tags?page=1&name=17.0.1-17.30.15-jre-headless
FROM azul/zulu-openjdk-alpine:17.0.1-17.30.15-jre-headless

RUN apk --no-cache upgrade && \
    apk add ffmpeg python3 && \
    ln -sf python3 /usr/bin/python

COPY --from=builder /opt/app.jar /opt/app.jar

WORKDIR /opt

RUN mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./youtube-dl && \
    chmod a+rx ./youtube-dl

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
