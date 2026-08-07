package com.seerah;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The editorial gate, over real HTTP: the public read path is open, writing needs
 * an editor, and only a scholar may sign off (§4.2, §6.5).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    private String url(String path) { return "http://localhost:" + port + path; }

    private Map<String, Object> newEvent() {
        return Map.of("slug", "sec-" + UUID.randomUUID(), "title", "Security Test Event",
                "hijriYear", 2, "gregorianYear", 624, "certainty", "WELL_ATTESTED",
                "major", false, "sortKey", 0);
    }

    @Test
    void publicReadsAreOpenToEveryone() {
        assertThat(rest.getForEntity(url("/api/public/timeline"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void writingRequiresAuthentication() {
        assertThat(rest.postForEntity(url("/api/events"), newEvent(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anEditorMayCreateContent() {
        assertThat(rest.withBasicAuth("editor", "editor-dev")
                .postForEntity(url("/api/events"), newEvent(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void aScholarMayNotCreateContent() {
        assertThat(rest.withBasicAuth("scholar", "scholar-dev")
                .postForEntity(url("/api/events"), newEvent(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyAScholarMaySignOff() {
        Map<String, Object> approval = Map.of(
                "targetId", UUID.randomUUID().toString(), "version", 1,
                "scholarEmail", "board@seerah.test", "scholarName", "Board", "note", "ok");

        // an editor is forbidden from approving
        assertThat(rest.withBasicAuth("editor", "editor-dev")
                .postForEntity(url("/api/review/events/approve"), approval, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // a scholar is allowed through the gate
        assertThat(rest.withBasicAuth("scholar", "scholar-dev")
                .postForEntity(url("/api/review/events/approve"), approval, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }
}
