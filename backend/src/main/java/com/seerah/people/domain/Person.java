package com.seerah.people.domain;

import com.seerah.platform.error.DomainRuleViolation;
import com.seerah.shared.ContentStatus;
import com.seerah.shared.Slug;

import java.time.Instant;

/**
 * The {@code person} aggregate (§9.3). Structurally the twin of
 * {@link com.seerah.content.domain.Event}: the same DRAFT → … → PUBLISHED
 * workflow and the same founding invariant — a person profile cannot be
 * published without a supporting citation (§13.2). Display names are not held
 * here; they live in {@code person_alias} (§11.2).
 */
public class Person {

    private final PersonId id;
    private Slug slug;
    private PersonRole role;
    private Lifespan lifespan;
    private String honorificKey;
    private ContentStatus status;
    private Instant publishedAt;
    private long version;

    private Person(PersonId id, Slug slug, PersonRole role, Lifespan lifespan,
                   String honorificKey, ContentStatus status, Instant publishedAt, long version) {
        this.id = id;
        this.slug = slug;
        this.role = role;
        this.lifespan = lifespan;
        this.honorificKey = honorificKey;
        this.status = status;
        this.publishedAt = publishedAt;
        this.version = version;
    }

    public static Person createDraft(PersonId id, Slug slug, PersonRole role,
                                     Lifespan lifespan, String honorificKey) {
        if (id == null || slug == null || role == null) {
            throw new IllegalArgumentException("id, slug and role are required");
        }
        return new Person(id, slug, role, lifespan == null ? Lifespan.unknown() : lifespan,
                honorificKey, ContentStatus.DRAFT, null, 0);
    }

    public static Person rehydrate(PersonId id, Slug slug, PersonRole role, Lifespan lifespan,
                                   String honorificKey, ContentStatus status,
                                   Instant publishedAt, long version) {
        return new Person(id, slug, role, lifespan, honorificKey, status, publishedAt, version);
    }

    public void submitForReview() {
        requireStatus("person.submit.illegal_state", ContentStatus.DRAFT, ContentStatus.CHANGES_REQUESTED);
        this.status = ContentStatus.IN_REVIEW;
    }

    public void approve() {
        requireStatus("person.approve.illegal_state", ContentStatus.IN_REVIEW);
        this.status = ContentStatus.APPROVED;
    }

    public void publish(long supportingCitationCount, Instant when) {
        requireStatus("person.publish.illegal_state", ContentStatus.APPROVED);
        if (supportingCitationCount < 1) {
            throw new DomainRuleViolation("person.publish.requires_citation",
                "A person profile cannot be published without at least one supporting citation.");
        }
        this.status = ContentStatus.PUBLISHED;
        this.publishedAt = when;
    }

    private void requireStatus(String code, ContentStatus... allowed) {
        for (ContentStatus s : allowed) {
            if (this.status == s) return;
        }
        throw new DomainRuleViolation(code, "Operation not permitted from status " + this.status + ".");
    }

    public PersonId id() { return id; }
    public Slug slug() { return slug; }
    public PersonRole role() { return role; }
    public Lifespan lifespan() { return lifespan; }
    public String honorificKey() { return honorificKey; }
    public ContentStatus status() { return status; }
    public Instant publishedAt() { return publishedAt; }
    public long version() { return version; }
}
