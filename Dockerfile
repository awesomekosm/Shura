FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.10_9

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk add ffmpeg python3 && \
    ln -sf python3 /usr/bin/python && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown appuser: /opt && \
    chmod u+w /opt

ADD --chown=appuser:appgroup target/shura-*.jar /opt/app.jar

WORKDIR /opt

USER appuser

RUN mkdir /opt/tools && \
    cd /opt/tools && \
    wget https://yt-dl.org/downloads/latest/youtube-dl -O ./youtube-dl && \
    chmod a+rx ./youtube-dl

ENV PATH=/opt/tools:$PATH

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
