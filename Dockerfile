# Multi-stage build for Family Space backend (amd64 + arm64)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew settings.docker.gradle.kts settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared ./shared
COPY backend ./backend
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :backend:installDist -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=builder /app/backend/build/install/backend /app
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app
EXPOSE 8080
CMD ["/app/bin/backend"]
