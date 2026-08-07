package com.seerah;

import com.seerah.search.adapter.in.scheduler.SearchProjectionRelay;
import com.seerah.search.api.SearchPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The search-phase pipeline end to end (§17): with {@code search.engine=opensearch},
 * seeding publishes events and people, the outbox relay projects them into a real
 * OpenSearch index, and search — through the same {@link SearchPort} the Postgres
 * engine implements — answers from OpenSearch.
 */
@SpringBootTest(properties = {
        "seerah.seed=true",
        "search.engine=opensearch",
        "search.relay.interval-ms=100000" // don't auto-tick; the test drives the relay
})
@Testcontainers
class OpenSearchSearchIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Container
    static final GenericContainer<?> OPENSEARCH =
            new GenericContainer<>("opensearchproject/opensearch:2.17.1")
                    .withExposedPorts(9200)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true") // plain HTTP, no auth (test only)
                    .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void openSearchUri(DynamicPropertyRegistry registry) {
        registry.add("search.opensearch.uri",
                () -> "http://" + OPENSEARCH.getHost() + ":" + OPENSEARCH.getMappedPort(9200));
    }

    @Autowired SearchProjectionRelay relay;
    @Autowired SearchPort search;

    @Test
    void publishedContentIsProjectedIntoOpenSearchAndFound() {
        // Drain the whole outbox (many batches): index every published event and person.
        int total = 0;
        for (int n = relay.drainOnce(); n > 0; n = relay.drainOnce()) {
            total += n;
        }
        assertThat(total).as("outbox rows processed by the relay").isGreaterThan(0);

        // an event, matched on its summary text
        assertThat(search.search("Badr", 10))
                .anyMatch(m -> m.type().equals("EVENT") && m.slug().equals("the-battle-of-badr"));
        assertThat(search.search("amnesty", 10))
                .anyMatch(m -> m.slug().equals("the-conquest-of-makkah"));

        // a person, matched on their name
        assertThat(search.search("Hamza", 10))
                .anyMatch(m -> m.type().equals("PERSON") && m.slug().equals("hamza"));

        // and nothing for a term outside the corpus
        assertThat(search.search("zzzznotacorpusword", 10)).isEmpty();
    }
}
