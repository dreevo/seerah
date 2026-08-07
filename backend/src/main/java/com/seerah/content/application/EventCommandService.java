package com.seerah.content.application;

import com.seerah.content.application.port.in.ApproveEventUseCase;
import com.seerah.content.application.port.in.CreateEventUseCase;
import com.seerah.content.application.port.in.PublishEventUseCase;
import com.seerah.content.application.port.in.SetEventTextUseCase;
import com.seerah.content.application.port.in.SubmitEventUseCase;
import com.seerah.content.application.port.out.EventTranslationPort;
import com.seerah.content.application.port.out.LoadEventPort;
import com.seerah.content.application.port.out.ProvenanceCheckPort;
import com.seerah.content.application.port.out.SaveEventPort;
import com.seerah.content.domain.Event;
import com.seerah.content.domain.EventId;
import com.seerah.content.domain.HistoricalDate;
import com.seerah.shared.Slug;
import com.seerah.platform.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The write-side application service for events (§23.3 — the application service
 * is the transaction boundary). It loads/mutates the aggregate and persists it.
 *
 * <p>The corpus is fixed and curated once by the seeder, so there is no runtime
 * event stream: no outbox, and no stale-approval trigger. The one invariant kept
 * is the real, timeless one — {@link Event#publish} refuses to publish an event
 * with no supporting citation (§13.2) or a SCHOLARS_DIFFER event with fewer than
 * two recorded positions (§13.4). Authenticity of the sources themselves is
 * checked at authoring time by the seed validator.
 */
@Service
@Transactional
public class EventCommandService
        implements CreateEventUseCase, SubmitEventUseCase, ApproveEventUseCase,
                   PublishEventUseCase, SetEventTextUseCase {

    private final SaveEventPort saveEventPort;
    private final LoadEventPort loadEventPort;
    private final EventTranslationPort translationPort;
    private final ProvenanceCheckPort provenanceCheckPort;

    public EventCommandService(SaveEventPort saveEventPort,
                               LoadEventPort loadEventPort,
                               EventTranslationPort translationPort,
                               ProvenanceCheckPort provenanceCheckPort) {
        this.saveEventPort = saveEventPort;
        this.loadEventPort = loadEventPort;
        this.translationPort = translationPort;
        this.provenanceCheckPort = provenanceCheckPort;
    }

    @Override
    public UUID create(Command c) {
        EventId id = EventId.newId();
        HistoricalDate date = HistoricalDate.hijriYear(c.hijriYear(), c.gregorianYear());
        Event event = Event.createDraft(id, new Slug(c.slug()), c.chronicleId(), date,
                c.certainty(), c.major(), c.sortKey());

        saveEventPort.save(event);
        translationPort.putValue(id.value(), "title", c.titleLocale(), c.title());
        return id.value();
    }

    @Override
    public void submit(UUID eventId) {
        Event event = loadOrThrow(eventId);
        event.submitForReview();
        saveEventPort.save(event);
    }

    @Override
    public void approve(UUID eventId) {
        Event event = loadOrThrow(eventId);
        event.approve();
        saveEventPort.save(event);
    }

    @Override
    public void publish(UUID eventId) {
        Event event = loadOrThrow(eventId);
        long citations = provenanceCheckPort.countSupportingCitations(eventId);
        int positions = provenanceCheckPort.countScholarlyPositions(eventId);
        event.publish(citations, positions, Instant.now());
        saveEventPort.save(event);
    }

    @Override
    public void setText(UUID eventId, String field, String locale, String value) {
        loadOrThrow(eventId); // ensure it exists
        translationPort.putValue(eventId, field, locale, value);
    }

    private Event loadOrThrow(UUID eventId) {
        return loadEventPort.load(EventId.of(eventId))
                .orElseThrow(() -> new NotFoundException("event.not_found",
                        "No event with id " + eventId));
    }
}
