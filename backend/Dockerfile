FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 aimusic

WORKDIR /app
COPY --from=build /workspace/build/install/ai-music-backend/ ./
RUN mkdir -p /app/uploads && chown -R aimusic:aimusic /app

USER aimusic
EXPOSE 8080
ENV PORT=8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:8080/ready || exit 1

ENTRYPOINT ["bin/ai-music-backend"]
