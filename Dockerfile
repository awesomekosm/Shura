# https://hub.docker.com/_/eclipse-temurin/?tab=tags&page=1&name=17-jdk-alpine
# Custom Java runtime using jlink in a multi-stage container build
FROM eclipse-temurin:17-jdk-alpine@sha256:8c1cea92d1928e0e068cd3b861fba4c0f5ee164a0490401d6bb3186329496401 as jre-build

# Create a custom Java runtime and patchelf with glibc to replace musl
RUN apk --no-cache upgrade && \
    apk --no-cache add git autoconf automake gcc g++ make gcompat && \
    git clone https://github.com/NixOS/patchelf.git && \
    cd patchelf && \
    ./bootstrap.sh && \
    ./configure --disable-dependency-tracking && \
    make && \
    make check && \
    make install && \
    $JAVA_HOME/bin/jlink \
         --add-modules java.se,jdk.crypto.ec \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime && \
    patchelf --set-interpreter /lib/ld-linux-x86-64.so.2 /javaruntime/bin/java

ADD target/shura-*.jar /javaruntime/app.jar

# https://hub.docker.com/_/alpine?tab=tags&page=1&name=3.15.0
# Define your base image
FROM alpine:3.15.3@sha256:1e014f84205d569a5cc3be4e108ca614055f7e21d11928946113ab3f36054801

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk --no-cache add ffmpeg python3 gcompat && \
    ln -sf python3 /usr/bin/python && \
    mkdir /opt/tools && \
    mkdir /opt/java && \
    cd /opt/tools && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./youtube-dl && \
    chmod a+rx ./youtube-dl && \
    mkdir /opt/cache

WORKDIR /opt

ENV JAVA_HOME=/opt/java
ENV PATH=/opt/tools:$JAVA_HOME/bin:$PATH
COPY --from=jre-build /javaruntime $JAVA_HOME

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/java/app.jar
