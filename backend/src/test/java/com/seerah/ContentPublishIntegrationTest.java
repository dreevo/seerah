package com.seerah;

import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.EventSummaryView;
import com.seerah.content.application.port.in.ApproveEventUseCase;
import com.seerah.content.application.port.in.CreateEventUseCase;
import com.seerah.content.application.port.in.PublishEventUseCase;
import com.seerah.content.application.port.in.SubmitEventUseCase;
import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.provenance.api.CitationRegistrar;
import com.seerah.shared.Certainty;
import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import com.seerah.shared.HadithGrade;
import com.seerah.shared.SourceTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The full vertical slice against a real Postgres (§23.4). Proves the platform's
 * timeless first principle end-to-end: an event cannot be published until it is
 * cited (§13.2), and a {@code SCHOLARS_DIFFER} event cannot be published until its
 * competing positions are on record (§13.4). These are the invariants that outlive
 * the (now removed) editorial pipeline — they are enforced in the aggregate.
 */
@SpringBootTest
@Testcontainers
class ContentPublishIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired CreateEventUseCase createEvent;
    @Autowired SubmitEventUseCase submitEvent;
    @Autowired ApproveEventUseCase approveEvent;
    @Autowired PublishEventUseCase publishEvent;
    @Autowired CitationRegistrar registrar;
    @Autowired EventReadPort eventRead;

    private UUID approvedEvent(String slug, Certainty certainty) {
        UUID id = createEvent.create(new CreateEventUseCase.Command(
                slug, "en", "Test: " + slug, 2, 624, certainty, true, 0));
        submitEvent.submit(id);
        approveEvent.approve(id);
        return id;
    }

    private void citeEvent(UUID eventId, String sourceSlug) {
        UUID sourceId = registrar.registerSource(new CitationRegistrar.RegisterSource(
                sourceSlug, "Sahih al-Bukhari", "al-Bukhari", SourceTier.PRIMARY, true));
        registrar.addCitation(new CitationRegistrar.AddCitation(
                sourceId, "Book of Maghazi, 3951", "HADITH", null, HadithGrade.SAHIH,
                EntityType.EVENT, eventId, CitationRole.SUPPORTS, null));
    }

    @Test
    void publishIsRejectedUntilTheEventIsCited() {
        UUID id = approvedEvent("badr-uncited", Certainty.WELL_ATTESTED);

        DomainRuleViolation ex = catchThrowableOfType(
                () -> publishEvent.publish(id), DomainRuleViolation.class);

        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.requires_citation");
        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("APPROVED");
    }

    @Test
    void publishSucceedsOnceCited() {
        UUID id = approvedEvent("badr-cited", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-1");

        publishEvent.publish(id);

        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("PUBLISHED");
    }

    @Test
    void scholarsDifferEventNeedsTwoPositionsToPublish() {
        UUID id = approvedEvent("elephant-year", Certainty.SCHOLARS_DIFFER);
        citeEvent(id, "bukhari-2");

        // Cited, but no positions yet → still blocked.
        DomainRuleViolation ex = catchThrowableOfType(
                () -> publishEvent.publish(id), DomainRuleViolation.class);
        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.scholars_differ_requires_positions");

        // Record two competing positions, then it may publish.
        registrar.addScholarlyPosition(new CitationRegistrar.AddScholarlyPosition(
                EntityType.EVENT, id, "date", "Ibn Ishaq", "Places it in one year.", null, 0));
        registrar.addScholarlyPosition(new CitationRegistrar.AddScholarlyPosition(
                EntityType.EVENT, id, "date", "al-Waqidi", "Places it in another.", null, 1));

        publishEvent.publish(id);
        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("PUBLISHED");
    }

    @Test
    void publishedEventsAppearOnTheTimeline() {
        UUID id = approvedEvent("uhud-timeline", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-3");
        publishEvent.publish(id);

        assertThat(eventRead.publishedTimeline("en"))
                .anyMatch(v -> v.id().equals(id) && v.title().equals("Test: uhud-timeline"));
    }
}
