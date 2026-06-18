# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-noble AS runtime

ENV TZ=Asia/Shanghai
ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system creatorspace \
    && useradd --system --gid creatorspace --home-dir /app --shell /usr/sbin/nologin creatorspace \
    && mkdir -p /app/storage/uploads \
    && chown -R creatorspace:creatorspace /app

COPY --from=build --chown=creatorspace:creatorspace /workspace/backend/target/creatorspace-backend-*.jar /app/app.jar

USER creatorspace

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/api/health" > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
