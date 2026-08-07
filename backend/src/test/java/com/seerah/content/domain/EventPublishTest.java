package com.seerah.content.domain;

import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.shared.Certainty;
import com.seerah.shared.ContentStatus;
import com.seerah.shared.Slug;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Pure-domain tests of the {@link Event} aggregate — no Spring, no database. These
 * pin the invariants the whole platform is built to protect (§13.2, §13.4).
 */
class EventPublishTest {

    private Event approvedEvent(Certainty certainty) {
        Event e = Event.createDraft(EventId.newId(), new Slug("battle-of-badr"),
                HistoricalDate.hijriYear(2, 624), certainty, true, 0);
        e.submitForReview();
        e.approve();
        return e;
    }

    @Test
    void newEventStartsAsDraft() {
        Event e = Event.createDraft(EventId.newId(), new Slug("the-hijrah"),
                HistoricalDate.hijriYear(1, 622), Certainty.MUTAWATIR, true, 0);
        assertThat(e.status()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void cannotPublishStraightFromDraft() {
        Event e = Event.createDraft(EventId.newId(), new Slug("the-hijrah"),
                HistoricalDate.hijriYear(1, 622), Certainty.MUTAWATIR, true, 0);
        assertThatThrownBy(() -> e.publish(5, 0, Instant.now()))
                .isInstanceOf(DomainRuleViolation.class);
    }

    @Test
    void publishWithoutCitationIsRejected() {
        Event e = approvedEvent(Certainty.WELL_ATTESTED);
        DomainRuleViolation ex = catchThrowableOfType(
                () -> e.publish(0, 0, Instant.now()), DomainRuleViolation.class);
        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.requires_citation");
        assertThat(e.status()).isEqualTo(ContentStatus.APPROVED); // unchanged
    }

    @Test
    void publishSucceedsWithAtLeastOneCitation() {
        Event e = approvedEvent(Certainty.WELL_ATTESTED);
        Instant now = Instant.now();
        e.publish(1, 0, now);
        assertThat(e.status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(e.publishedAt()).isEqualTo(now);
    }

    @Test
    void scholarsDifferRequiresTwoPositions() {
        Event e = approvedEvent(Certainty.SCHOLARS_DIFFER);
        DomainRuleViolation ex = catchThrowableOfType(
                () -> e.publish(3, 1, Instant.now()), DomainRuleViolation.class);
        assertThat(ex).isNotNull();
        assertThat(ex.code()).isEqualTo("event.publish.scholars_differ_requires_positions");
    }

    @Test
    void scholarsDifferPublishesWithTwoPositions() {
        Event e = approvedEvent(Certainty.SCHOLARS_DIFFER);
        e.publish(3, 2, Instant.now());
        assertThat(e.status()).isEqualTo(ContentStatus.PUBLISHED);
    }
}
