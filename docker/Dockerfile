#
# SPDX-License-Identifier: Apache-2.0
# © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
# attributed to the Department for Business and Trade (UK) as the governing entity.
#

# -------------------------------------------------------------------
# Build stage
# Base image pulled from private ACR via BASE_REGISTRY build arg
# -------------------------------------------------------------------
ARG BASE_REGISTRY
FROM ${BASE_REGISTRY}/maven:3.9.6-eclipse-temurin-21-alpine AS build
RUN apk update && apk upgrade --no-cache

WORKDIR /build

# Copy project files
# Build context is repo root (.),  so paths are relative to that
COPY pom.xml .
COPY src ./src

# Build the application — tests are skipped here; they run in the CI unit-test stage
RUN mvn -B clean package -DskipTests

# -------------------------------------------------------------------
# Runtime stage
# Name matches --target in CI for clarity; base image from private ACR
# -------------------------------------------------------------------
FROM ${BASE_REGISTRY}/eclipse-temurin:21-jdk-alpine AS federator-certificate-manager
RUN apk update && apk upgrade --no-cache

# Create non-root user and group
RUN addgroup -S app && adduser -S -G app -u 10001 app

WORKDIR /app

# Create required directories with correct ownership
RUN mkdir -p /app/docker /app/logs /app/tmp && chown -R app:app /app

# JAR_NAME is passed in by CI as  federator-certificate-manager-<version>
# e.g.  --build-arg JAR_NAME=federator-certificate-manager-0.90.0
ARG JAR_NAME
COPY --from=build /build/target/${JAR_NAME}.jar /app/app.jar

RUN chown app:app /app/app.jar

# Drop to non-root user
USER app:app

# Helpful JVM defaults for containerised workloads
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.io.tmpdir=/app/tmp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
