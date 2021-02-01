FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.10_9@sha256:5f2dbc41231178d8e4a1da9dc8a489808729918892a62a2c83c7570ab1785d60 AS builder

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    apk add git && \
    git --version && \
    cd /opt && \
    git clone -b removed-springboot https://github.com/Z-EMB/Shura.git && \
    cd Shura && \
    ./mvnw package && \
    cp /opt/Shura/target/shura-*.jar /opt/app.jar && \
    rm -rf /opt/shura-source

FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.10_9@sha256:5f2dbc41231178d8e4a1da9dc8a489808729918892a62a2c83c7570ab1785d60

RUN apk --no-cache upgrade && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown appuser: /opt && \
    chmod u+w /opt

COPY --from=builder --chown=appuser:appgroup /opt/app.jar /opt/app.jar

WORKDIR /opt

USER appuser

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
