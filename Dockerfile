FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home shoplink
WORKDIR /app
ARG SERVICE
COPY --from=build --chown=shoplink:shoplink /workspace/${SERVICE}/target/*-exec.jar app.jar
USER shoplink
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:ActiveProcessorCount=2"
ENTRYPOINT ["java", "-jar", "app.jar"]
