# Multi-stage docker build on Java 17: Build in first container, run in jre-only container
FROM eclipse-temurin:17 as builder

WORKDIR /app
COPY . .
RUN ./gradlew clean
RUN ./gradlew assemble

FROM eclipse-temurin:17-jre as runner

WORKDIR /app
COPY --from=builder /app/build/libs/hap-ibd.jar /app/hap-ibd.jar
CMD ["java", "-jar", "/app/hap-ibd.jar"]
