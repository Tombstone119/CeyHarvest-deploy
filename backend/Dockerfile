# syntax=docker/dockerfile:1

# ---- build ----------------------------------------------------------------
# Pinned to JDK 21: the project targets Java 21 and Lombok 1.18.30 does not
# support JDK 23+ (it fails with "TypeTag :: UNKNOWN" during compilation).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first so they stay cached when only sources change
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests boot the full Spring context and need a reachable MongoDB, so they are
# not part of the image build.
RUN mvn -B -q clean package -DskipTests

# ---- runtime --------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

EXPOSE 8080

# Container memory is small on free tiers; let the JVM size the heap from the
# cgroup limit rather than the host's total RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
