# https://hub.docker.com/_/eclipse-temurin/?tab=tags&page=1&name=17-jdk-alpine
# Custom Java runtime using jlink in a multi-stage container build
FROM eclipse-temurin:17-jdk-alpine@sha256:b30fa3ce4323ce037cb95fd2729dd4662d86f0ee2986452527cc645eaf258a1d as jre-build

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
FROM alpine:3.15.0@sha256:e7d88de73db3d3fd9b2d63aa7f447a10fd0220b7cbf39803c803f2af9ba256b3

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
