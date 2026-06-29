FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace/backend

RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml << 'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

FROM eclipse-temurin:21-jre-alpine AS runtime

ENV TZ=Asia/Shanghai
ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

WORKDIR /app

RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && addgroup -S creatorspace \
    && adduser -S -G creatorspace -h /app -s /sbin/nologin creatorspace \
    && mkdir -p /app/storage/uploads \
    && chown -R creatorspace:creatorspace /app

COPY --from=build --chown=creatorspace:creatorspace /workspace/backend/target/creatorspace-backend-*.jar /app/app.jar

USER creatorspace

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/api/health" > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
