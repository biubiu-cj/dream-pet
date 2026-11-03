# syntax=docker/dockerfile:1
# DreamPet 后端镜像（CloudBase 云托管 / 任意容器平台通用）
# 构建: docker build -t dreampet-backend .
# 运行: docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e DB_HOST=... dreampet-backend

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1000 dreampet && chown -R dreampet /app
USER dreampet
COPY --from=build /build/target/*.jar /app/app.jar

# 云托管可通过环境变量 PORT 注入端口（默认 8080）
ENV PORT=8080
EXPOSE 8080

# 适配云托管小实例：限制堆内存、使用单线程 GC 降低开销
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]