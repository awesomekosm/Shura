FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.10_9 AS builder

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

FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.10_9

RUN apk --no-cache upgrade && \
    apk add ffmpeg python3 && \
    ln -sf python3 /usr/bin/python

COPY --from=builder /opt/app.jar /opt/app.jar

WORKDIR /opt

RUN mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://yt-dl.org/downloads/latest/youtube-dl -O ./youtube-dl && \
    chmod a+rx ./youtube-dl

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
