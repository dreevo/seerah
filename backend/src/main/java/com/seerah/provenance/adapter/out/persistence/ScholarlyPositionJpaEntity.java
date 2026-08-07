package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
import com.seerah.shared.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/**
 * One distinct scholarly view on a disputed point (§12.6 {@code scholarly_position}).
 * When a claim is marked {@code SCHOLARS_DIFFER}, two or more of these must exist
 * before it can be published (§13.4) — the platform shows disagreement rather than
 * silently resolving it.
 */
@Entity
@Table(name = "scholarly_position")
public class ScholarlyPositionJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "target_type", nullable = false, columnDefinition = "entity_type")
    private EntityType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "position_key", nullable = false)
    private String positionKey;

    @Column(name = "held_by", nullable = false)
    private String heldBy;

    @Column(nullable = false)
    private String summary;

    @Column(name = "citation_id")
    private UUID citationId;

    @Column(nullable = false)
    private int ordinal;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ScholarlyPositionJpaEntity() { }

    public static ScholarlyPositionJpaEntity create(EntityType targetType, UUID targetId,
                                                    String positionKey, String heldBy,
                                                    String summary, UUID citationId, int ordinal) {
        ScholarlyPositionJpaEntity p = new ScholarlyPositionJpaEntity();
        p.id = UUID.randomUUID();
        p.targetType = targetType;
        p.targetId = targetId;
        p.positionKey = positionKey;
        p.heldBy = heldBy;
        p.summary = summary;
        p.citationId = citationId;
        p.ordinal = ordinal;
        return p;
    }

    public UUID getId() { return id; }
}
