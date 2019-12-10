FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.5_10

ARG JAVA_OPTS

RUN apk add git && \
    git --version && \
    cd /opt && \
    git clone https://github.com/Z-EMB/Shura.git && \
    cd Shura && \
    ./gradlew --no-daemon bootJar && \
    cp /opt/Shura/build/libs/shura-*.jar /opt/app.jar && \
    rm -rf /opt/shura-source

WORKDIR /opt

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
