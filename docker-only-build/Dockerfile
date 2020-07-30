FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.7_10 AS builder

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

FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.7_10

RUN apk --no-cache upgrade && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown appuser: /opt && \
    chmod u+w /opt

COPY --from=builder --chown=appuser:appgroup /opt/app.jar /opt/app.jar

WORKDIR /opt

USER appuser

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
