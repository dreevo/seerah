package com.seerah.content.adapter.in.web;

import com.seerah.content.adapter.in.web.dto.CreateEventRequest;
import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.EventSummaryView;
import com.seerah.content.application.port.in.ApproveEventUseCase;
import com.seerah.content.application.port.in.CreateEventUseCase;
import com.seerah.content.application.port.in.PublishEventUseCase;
import com.seerah.content.application.port.in.SubmitEventUseCase;
import com.seerah.platform.error.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * The inbound web adapter for events (§23.4 — the controller is the outermost
 * ring; it maps HTTP to use-case calls and holds no business logic).
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final CreateEventUseCase createEvent;
    private final SubmitEventUseCase submitEvent;
    private final ApproveEventUseCase approveEvent;
    private final PublishEventUseCase publishEvent;
    private final EventReadPort eventRead;

    public EventController(CreateEventUseCase createEvent, SubmitEventUseCase submitEvent,
                           ApproveEventUseCase approveEvent, PublishEventUseCase publishEvent,
                           EventReadPort eventRead) {
        this.createEvent = createEvent;
        this.submitEvent = submitEvent;
        this.approveEvent = approveEvent;
        this.publishEvent = publishEvent;
        this.eventRead = eventRead;
    }

    @PostMapping
    public ResponseEntity<EventSummaryView> create(@Valid @RequestBody CreateEventRequest req) {
        UUID id = createEvent.create(new CreateEventUseCase.Command(
                req.slug(), req.localeOrDefault(), req.title(),
                req.hijriYear(), req.gregorianYear(), req.certainty(), req.major(), req.sortKey()));
        EventSummaryView view = eventRead.findById(id, req.localeOrDefault())
                .orElseThrow(() -> new NotFoundException("event.not_found", "Event vanished after creation"));
        return ResponseEntity.created(URI.create("/api/events/" + req.slug())).body(view);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submit(@PathVariable UUID id) {
        submitEvent.submit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id) {
        approveEvent.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) {
        publishEvent.publish(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{slug}")
    public EventSummaryView bySlug(@PathVariable String slug,
                                   @RequestParam(defaultValue = "en") String locale) {
        return eventRead.findBySlug(slug, locale)
                .orElseThrow(() -> new NotFoundException("event.not_found", "No event with slug " + slug));
    }

    @GetMapping
    public List<EventSummaryView> timeline(@RequestParam(defaultValue = "en") String locale) {
        return eventRead.publishedTimeline(locale);
    }
}
