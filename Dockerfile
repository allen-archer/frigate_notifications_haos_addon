ARG BUILD_FROM
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY src/main/ src/main/
RUN chmod +x gradlew && ./gradlew copyDependencies jar -x test

ARG JAVA_BASE=eclipse-temurin:17-jre
FROM $JAVA_BASE AS java-provider

FROM $BUILD_FROM
COPY --from=java-provider /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY run.sh /run.sh
RUN chmod a+x /run.sh
COPY --from=builder /build/build/libs/deps/ /deps/
COPY --from=builder /build/build/libs/app.jar /app.jar
CMD ["/run.sh"]