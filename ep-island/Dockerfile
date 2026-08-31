FROM node:24-alpine AS frontend

WORKDIR /workspace
COPY frontend/package.json frontend/pnpm-lock.yaml ./frontend/
RUN corepack enable && cd frontend && pnpm install --frozen-lockfile

COPY frontend ./frontend
COPY src/main/resources/static ./src/main/resources/static
RUN cd frontend && pnpm build

FROM maven:3.9.11-eclipse-temurin-21-alpine AS backend

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
COPY --from=frontend /workspace/src/main/resources/static/react ./src/main/resources/static/react
RUN mvn -B clean package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S episland && adduser -S episland -G episland
WORKDIR /app
COPY --from=backend /workspace/target/ep-island-*.jar app.jar

USER episland
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
