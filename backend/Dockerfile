# ---- build stage: compile the Spring Boot fat jar with the Gradle wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Warm the dependency cache first (rebuilds are fast unless build files change).
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies >/dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon bootJar

# ---- run stage: a slim JRE with just the jar --------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-SNAPSHOT.jar /app/seerah.jar
EXPOSE 8080
# Virtual-thread-friendly; the app reads DB_URL/DB_USER/DB_PASSWORD from the env.
ENTRYPOINT ["java", "-jar", "/app/seerah.jar"]
