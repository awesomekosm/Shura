FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.7_10

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown appuser: /opt && \
    chmod u+w /opt

ADD --chown=appuser:appgroup target/shura-*.jar /opt/app.jar

WORKDIR /opt

USER appuser

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar
