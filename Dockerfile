FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.7_10

ARG JAVA_OPTS

RUN apk --no-cache upgrade && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup

ADD --chown=appuser:appgroup build/libs/shura-*.jar /opt/app.jar
RUN chmod 777 /opt

WORKDIR /opt

USER appuser

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/app.jar