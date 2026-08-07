package com.seerah.content.domain;

import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.shared.Certainty;
import com.seerah.shared.ContentStatus;
import com.seerah.shared.Slug;

import java.time.Instant;
import java.util.UUID;

/**
 * The central content aggregate (§9.2). One instance per historical event.
 *
 * <p>Note what it does <em>not</em> hold: title, summary, body. Those are
 * localisable strings and live in the {@code translation} table (§11.2), so the
 * aggregate carries only structural and editorial facts.
 *
 * <p>The aggregate is the transaction boundary and the guardian of the platform's
 * founding invariants. Two are enforced here, at {@link #publish}:
 * <ul>
 *   <li><b>§13.2 citation-required</b> — nothing reaches the public without at
 *       least one supporting citation;</li>
 *   <li><b>§13.4 scholars-differ</b> — a claim marked {@code SCHOLARS_DIFFER}
 *       must expose two or more scholarly positions.</li>
 * </ul>
 * The database enforces the same rules independently (§13); belt and braces.
 */
public class Event {

    private final EventId id;
    private Slug slug;
    private UUID chronicleId;
    private HistoricalDate date;
    private Certainty certainty;
    private ContentStatus status;
    private boolean major;
    private int sortKey;
    private Instant publishedAt;
    private long version;

    private Event(EventId id, Slug slug, UUID chronicleId, HistoricalDate date, Certainty certainty,
                  ContentStatus status, boolean major, int sortKey,
                  Instant publishedAt, long version) {
        this.id = id;
        this.slug = slug;
        this.chronicleId = chronicleId;
        this.date = date;
        this.certainty = certainty;
        this.status = status;
        this.major = major;
        this.sortKey = sortKey;
        this.publishedAt = publishedAt;
        this.version = version;
    }

    /** Create a brand-new event in {@link ContentStatus#DRAFT}, in a chronicle. */
    public static Event createDraft(EventId id, Slug slug, UUID chronicleId, HistoricalDate date,
                                    Certainty certainty, boolean major, int sortKey) {
        if (id == null || slug == null || date == null || certainty == null) {
            throw new IllegalArgumentException("id, slug, date and certainty are required");
        }
        return new Event(id, slug, chronicleId, date, certainty, ContentStatus.DRAFT, major, sortKey, null, 0);
    }

    /** Overload without a chronicle (defaults to none) — used by unit tests. */
    public static Event createDraft(EventId id, Slug slug, HistoricalDate date,
                                    Certainty certainty, boolean major, int sortKey) {
        return createDraft(id, slug, null, date, certainty, major, sortKey);
    }

    /** Rebuild an event from persisted state. No invariants re-run: the store is trusted. */
    public static Event rehydrate(EventId id, Slug slug, UUID chronicleId, HistoricalDate date, Certainty certainty,
                                  ContentStatus status, boolean major, int sortKey,
                                  Instant publishedAt, long version) {
        return new Event(id, slug, chronicleId, date, certainty, status, major, sortKey, publishedAt, version);
    }

    // --- editorial workflow (§8.5 event storming; DRAFT → … → PUBLISHED) -----

    public void submitForReview() {
        requireStatus("event.submit.illegal_state", ContentStatus.DRAFT, ContentStatus.CHANGES_REQUESTED);
        this.status = ContentStatus.IN_REVIEW;
    }

    public void approve() {
        requireStatus("event.approve.illegal_state", ContentStatus.IN_REVIEW);
        this.status = ContentStatus.APPROVED;
    }

    public void requestChanges() {
        requireStatus("event.request_changes.illegal_state", ContentStatus.IN_REVIEW);
        this.status = ContentStatus.CHANGES_REQUESTED;
    }

    /**
     * Publish the event, enforcing the provenance invariants.
     *
     * @param supportingCitationCount number of supporting citation links on this event (§13.2)
     * @param scholarlyPositionCount  number of recorded scholarly positions (§13.4)
     * @param when                    the publication instant
     */
    public void publish(long supportingCitationCount, int scholarlyPositionCount, Instant when) {
        requireStatus("event.publish.illegal_state", ContentStatus.APPROVED);

        if (supportingCitationCount < 1) {
            throw new DomainRuleViolation("event.publish.requires_citation",
                "An event cannot be published without at least one supporting citation.");
        }
        if (certainty.requiresPositions() && scholarlyPositionCount < 2) {
            throw new DomainRuleViolation("event.publish.scholars_differ_requires_positions",
                "An event marked SCHOLARS_DIFFER must record at least two scholarly positions "
                + "before it can be published.");
        }

        this.status = ContentStatus.PUBLISHED;
        this.publishedAt = when;
    }

    public void retire() {
        requireStatus("event.retire.illegal_state", ContentStatus.PUBLISHED);
        this.status = ContentStatus.RETIRED;
    }

    private void requireStatus(String code, ContentStatus... allowed) {
        for (ContentStatus s : allowed) {
            if (this.status == s) return;
        }
        throw new DomainRuleViolation(code,
            "Operation not permitted from status " + this.status + ".");
    }

    // --- accessors (no public setters: mutation only via behaviour) ----------

    public EventId id() { return id; }
    public Slug slug() { return slug; }
    public UUID chronicleId() { return chronicleId; }
    public HistoricalDate date() { return date; }
    public Certainty certainty() { return certainty; }
    public ContentStatus status() { return status; }
    public boolean isMajor() { return major; }
    public int sortKey() { return sortKey; }
    public Instant publishedAt() { return publishedAt; }
    public long version() { return version; }
}
