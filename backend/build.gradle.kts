// ============================================================================
//  Seerah Platform — Phase 1 modular monolith
//  Java 21 · Spring Boot 3 · PostgreSQL  (per the Technical Design Record)
//
//  One deployable. Bounded contexts are packages under `com.seerah`, with
//  boundaries enforced by the ArchUnit test suite rather than by build modules
//  (§21 "the modular monolith", §22 "module structure and boundary enforcement").
// ============================================================================
plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.seerah"
version = "0.1.0-SNAPSHOT"

// Spring Boot 3.3 manages Testcontainers 1.19.x, whose docker-java negotiates an
// API version that Docker Engine 29 (min API 1.40) rejects. Bump it to a build
// that speaks to modern Docker.
extra["testcontainers.version"] = "1.20.6"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- web / api ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- persistence: Postgres is the single source of truth (§5.4) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- operations ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- security: role-based gate on the editorial endpoints (§4.2, §6.5) ---
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- search phase (§17): OpenSearch, behind the SearchPort, flag-selected ---
    implementation("org.opensearch.client:opensearch-rest-client:2.19.6")

    // --- test: real Postgres via Testcontainers, boundaries via ArchUnit ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Point Testcontainers at Docker Desktop's socket without hardcoding a path,
    // and skip Ryuk (flaky on Docker Desktop). Only set DOCKER_HOST if the caller
    // has not already provided one.
    if (System.getenv("DOCKER_HOST") == null) {
        val sock = "${System.getProperty("user.home")}/.docker/run/docker.sock"
        if (file(sock).exists()) {
            environment("DOCKER_HOST", "unix://$sock")
        }
    }
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    // Pin the Docker Engine API version: docker-java negotiates a version newer
    // than Docker Engine 29 will serve, which it answers with HTTP 400.
    systemProperty("api.version", "1.44")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}
