ARG BUILD_FROM
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY src/main/ src/main/
RUN chmod +x gradlew && ./gradlew copyDependencies jar -x test

FROM $BUILD_FROM
RUN apk add --no-cache openjdk17-jre-headless
COPY run.sh /run.sh
RUN chmod a+x /run.sh
COPY --from=builder /build/build/libs/deps/ /deps/
COPY --from=builder /build/build/libs/app.jar /app.jar
CMD ["/run.sh"]