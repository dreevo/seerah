package com.seerah;

import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.EventSummaryView;
import com.seerah.content.application.port.in.ApproveEventUseCase;
import com.seerah.content.application.port.in.CreateEventUseCase;
import com.seerah.content.application.port.in.PublishEventUseCase;
import com.seerah.content.application.port.in.SetEventTextUseCase;
import com.seerah.content.application.port.in.SubmitEventUseCase;
import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.platform.outbox.OutboxJpaRepository;
import com.seerah.provenance.api.CitationRegistrar;
import com.seerah.review.api.ReviewRegistrar;
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
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The full vertical slice against a real Postgres (§23.4). Proves the platform's
 * first principle end-to-end: an event cannot be published until it is cited, and
 * a {@code SCHOLARS_DIFFER} event cannot be published until its positions are on
 * record — and that a successful publish emits a transactional-outbox event.
 */
@SpringBootTest
@Testcontainers
class ContentPublishIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired CreateEventUseCase createEvent;
    @Autowired SubmitEventUseCase submitEvent;
    @Autowired ApproveEventUseCase approveEvent;
    @Autowired PublishEventUseCase publishEvent;
    @Autowired CitationRegistrar registrar;
    @Autowired ReviewRegistrar review;
    @Autowired SetEventTextUseCase setText;
    @Autowired EventReadPort eventRead;
    @Autowired OutboxJpaRepository outbox;

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

    private void scholarApproves(UUID eventId) {
        review.approve(EntityType.EVENT, eventId, 1, "scholar@seerah.platform", "Dr. Test", "looks sound");
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
    void publishSucceedsOnceCitedAndEmitsOutboxEvent() {
        UUID id = approvedEvent("badr-cited", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-1");
        scholarApproves(id);

        publishEvent.publish(id);

        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("PUBLISHED");
        assertThat(outbox.findAll())
                .anyMatch(r -> r.getAggregateId().equals(id)
                        && r.getEventType().equals("content.event.published.v1"));
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

        scholarApproves(id);
        publishEvent.publish(id);
        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("PUBLISHED");
    }

    @Test
    void publishedEventsAppearOnTheTimeline() {
        UUID id = approvedEvent("uhud-timeline", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-3");
        scholarApproves(id);
        publishEvent.publish(id);

        assertThat(eventRead.publishedTimeline("en"))
                .anyMatch(v -> v.id().equals(id) && v.title().equals("Test: uhud-timeline"));
    }

    @Test
    void publishIsRejectedWithoutAnyScholarlyApproval() {
        // Cited, but never reviewed → the DB stale-approval trigger blocks publish (§13.6).
        UUID id = approvedEvent("badr-unreviewed", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-nr");

        DomainRuleViolation ex = catchThrowableOfType(
                () -> publishEvent.publish(id), DomainRuleViolation.class);
        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.stale_approval");
    }

    @Test
    void silentEditAfterApprovalMakesTheApprovalStale() {
        UUID id = approvedEvent("badr-stale", Certainty.WELL_ATTESTED);
        citeEvent(id, "bukhari-stale");
        scholarApproves(id);

        // An editor silently changes the summary after the scholar signed off.
        setText.setText(id, "summary", "en", "an edit the scholar never saw");

        DomainRuleViolation ex = catchThrowableOfType(
                () -> publishEvent.publish(id), DomainRuleViolation.class);
        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.stale_approval");

        // Re-approval of the new content unblocks it.
        scholarApproves(id);
        publishEvent.publish(id);
        assertThat(eventRead.findById(id, "en")).get()
                .extracting(EventSummaryView::status).isEqualTo("PUBLISHED");
    }
}
