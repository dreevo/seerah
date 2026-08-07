package com.seerah.content.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.platform.error.NotFoundException;
import com.seerah.platform.outbox.OutboxRecorder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The write-side application service for events (§23.3 — the application service
 * is the transaction boundary). It orchestrates: load/mutate the aggregate,
 * persist it, and record an outbox event — all in one transaction, so the state
 * change and its announcement commit atomically (§25.5).
 *
 * <p>One service implements several inbound ports; that is deliberate — the
 * ports are the contract, the service is an implementation detail.
 */
@Service
@Transactional
public class EventCommandService
        implements CreateEventUseCase, SubmitEventUseCase, ApproveEventUseCase,
                   PublishEventUseCase, SetEventTextUseCase {

    private static final String AGGREGATE = "event";

    private final SaveEventPort saveEventPort;
    private final LoadEventPort loadEventPort;
    private final EventTranslationPort translationPort;
    private final ProvenanceCheckPort provenanceCheckPort;
    private final OutboxRecorder outbox;
    private final ObjectMapper json;

    public EventCommandService(SaveEventPort saveEventPort,
                               LoadEventPort loadEventPort,
                               EventTranslationPort translationPort,
                               ProvenanceCheckPort provenanceCheckPort,
                               OutboxRecorder outbox,
                               ObjectMapper json) {
        this.saveEventPort = saveEventPort;
        this.loadEventPort = loadEventPort;
        this.translationPort = translationPort;
        this.provenanceCheckPort = provenanceCheckPort;
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public UUID create(Command c) {
        EventId id = EventId.newId();
        HistoricalDate date = HistoricalDate.hijriYear(c.hijriYear(), c.gregorianYear());
        Event event = Event.createDraft(id, new Slug(c.slug()), c.chronicleId(), date,
                c.certainty(), c.major(), c.sortKey());

        saveEventPort.save(event);
        translationPort.putValue(id.value(), "title", c.titleLocale(), c.title());

        emit(id.value(), "content.event.created.v1", n -> {
            n.put("slug", c.slug());
            n.put("certainty", c.certainty().name());
        });
        return id.value();
    }

    @Override
    public void submit(UUID eventId) {
        Event event = loadOrThrow(eventId);
        event.submitForReview();
        saveEventPort.save(event);
        emit(eventId, "content.event.submitted.v1", n -> { });
    }

    @Override
    public void approve(UUID eventId) {
        Event event = loadOrThrow(eventId);
        event.approve();
        saveEventPort.save(event);
        emit(eventId, "content.event.approved.v1", n -> { });
    }

    @Override
    public void publish(UUID eventId) {
        Event event = loadOrThrow(eventId);

        long citations = provenanceCheckPort.countSupportingCitations(eventId);
        int positions = provenanceCheckPort.countScholarlyPositions(eventId);

        // The aggregate checks the provenance invariants in Java (§13.2, §13.4)…
        event.publish(citations, positions, Instant.now());

        // …then the database's stale-approval trigger has the final word (§13.6):
        // publishing is refused unless a scholar's approval still matches the content.
        try {
            saveEventPort.save(event);
        } catch (DataAccessException ex) {
            if (isStaleApproval(ex)) {
                throw new DomainRuleViolation("event.publish.stale_approval",
                    "This event has no scholarly approval matching its current content. "
                    + "It changed after review and must be re-approved before publishing.");
            }
            throw ex;
        }
        emit(eventId, "content.event.published.v1", n -> n.put("supportingCitations", citations));
    }

    private static boolean isStaleApproval(DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains("STALE_APPROVAL");
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

    private void emit(UUID id, String type, java.util.function.Consumer<ObjectNode> body) {
        ObjectNode payload = json.createObjectNode();
        payload.put("eventId", id.toString());
        body.accept(payload);
        outbox.record(AGGREGATE, id, type, payload.toString());
    }
}
