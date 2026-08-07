package com.seerah;

import com.seerah.publicapi.PublicReadController;
import com.seerah.publicapi.PublicViews.EventDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof of the connected chronology. Boots the app with seeding on,
 * so the real create → cite → publish flow populates a live graph, then reads it
 * back through the public BFF exactly as the Angular app will.
 */
@SpringBootTest(properties = "seerah.seed=true")
@Testcontainers
class PublicApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired PublicReadController publicApi;

    @Test
    void timelineReturnsThePublishedSeededEvents() {
        assertThat(publicApi.timeline("en", null))
                .extracting(i -> i.slug())
                .contains("birth-of-the-prophet", "the-first-revelation", "the-hijrah-to-madinah",
                        "the-battle-of-badr", "treaty-of-hudaybiyyah", "the-conquest-of-makkah");
    }

    @Test
    void badrDetailIsFullyConnected() {
        EventDetail badr = publicApi.event("the-battle-of-badr", "en");

        assertThat(badr.title()).isEqualTo("The Battle of Badr");
        assertThat(badr.summary()).contains("wells of Badr");

        // people who took part
        assertThat(badr.people()).extracting(p -> p.name())
                .contains("Hamza ibn Abd al-Muttalib", "Ali ibn Abi Talib");

        // a verse revealed about it, with verbatim Arabic and a named translation
        assertThat(badr.verses()).hasSize(1);
        assertThat(badr.verses().get(0).reference()).isEqualTo("8:17");
        assertThat(badr.verses().get(0).translation()).contains("it was Allah who threw");

        // its neighbours in the chronology: the Hijrah before it, Uhud after it
        assertThat(badr.relatedEvents()).extracting(e -> e.slug())
                .contains("the-hijrah-to-madinah", "the-battle-of-uhud");

        // where it happened
        assertThat(badr.places()).extracting(p -> p.name()).contains("Badr");
        assertThat(badr.places().get(0).latitude()).isNotNull();

        // it is cited: publishing is impossible without a supporting source (§13.2),
        // and its sources include the sīra plus a modern secondary work
        assertThat(badr.sources()).isNotEmpty();
        assertThat(badr.sources()).extracting(s -> s.workTitle())
                .contains("As-Sirah an-Nabawiyyah", "Ar-Raheeq Al-Makhtum");
    }

    @Test
    void companionsIndexAndPersonDetailAreConnected() {
        assertThat(publicApi.companions("en", null)).extracting(p -> p.slug())
                .contains("abu-bakr", "hamza", "ali-ibn-abi-talib");

        var hamza = publicApi.person("hamza", "en");
        assertThat(hamza.name()).isEqualTo("Hamza ibn Abd al-Muttalib");
        // Hamza appears at Badr — the reverse relationship lookup finds it.
        assertThat(hamza.events()).extracting(e -> e.slug()).contains("the-battle-of-badr");
    }

    @Test
    void searchRanksByMeaningNotJustKeywords() {
        // an exact term still works — the event whose title is Badr
        assertThat(publicApi.search("Badr", "en", null))
                .anyMatch(h -> h.type().equals("EVENT") && h.slug().equals("the-battle-of-badr"));
        // a person by name
        assertThat(publicApi.search("Hamza", "en", null))
                .anyMatch(h -> h.type().equals("PERSON") && h.slug().equals("hamza"));
        // MEANING over keywords: this query shares almost no content words with the
        // Hijrah summary, yet semantic search still surfaces it
        assertThat(publicApi.search("emigrating from Makkah to Yathrib to escape those plotting against him", "en", null))
                .anyMatch(h -> h.slug().equals("the-hijrah-to-madinah"));
    }

    @Test
    void learningPathsListAndResolveTheirSteps() {
        assertThat(publicApi.paths("en", null)).extracting(p -> p.slug())
                .contains("start-here", "the-hijrah-and-after");

        var path = publicApi.path("start-here", "en");
        assertThat(path.title()).isEqualTo("Start Here — The Pivotal Events");
        assertThat(path.steps()).extracting(s -> s.eventSlug())
                .containsExactly("birth-of-the-prophet", "the-first-revelation",
                        "the-hijrah-to-madinah", "the-battle-of-badr",
                        "treaty-of-hudaybiyyah", "the-conquest-of-makkah");
        // steps carry the localised event title, resolved from the event read port
        assertThat(path.steps().get(3).eventTitle()).isEqualTo("The Battle of Badr");
    }

    @Test
    void hijrahDetailHasAMappedRoute() {
        var hijrah = publicApi.event("the-hijrah-to-madinah", "en");
        assertThat(hijrah.routes()).isNotEmpty();
        var route = hijrah.routes().get(0);
        assertThat(route.points()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(route.conjectural()).isFalse();
        assertThat(route.distanceKm()).isNotNull();
        assertThat(route.distanceKm()).isGreaterThan(100.0); // Makkah→Madinah is ~300+ km
    }

    @Test
    void seededPeopleAreCitedAndPublished() {
        // Khadijah took part in the first revelation and is a published, cited profile.
        EventDetail rev = publicApi.event("the-first-revelation", "en");
        assertThat(rev.people()).extracting(p -> p.slug()).contains("khadijah");
        assertThat(rev.verses()).extracting(v -> v.reference()).contains("96:1");
    }
}
