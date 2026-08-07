package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.CitationRole;
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
 * Links a citation to something it supports (§12.6 {@code citation_link}). The
 * {@code fieldName} lets a citation support one specific claim within an entity —
 * the date, say, rather than the whole event.
 */
@Entity
@Table(name = "citation_link")
public class CitationLinkJpaEntity {

    @Id
    private UUID id;

    @Column(name = "citation_id", nullable = false)
    private UUID citationId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "target_type", nullable = false, columnDefinition = "entity_type")
    private EntityType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "citation_role")
    private CitationRole role;

    @Column(name = "field_name")
    private String fieldName;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CitationLinkJpaEntity() { }

    public static CitationLinkJpaEntity create(UUID citationId, EntityType targetType,
                                               UUID targetId, CitationRole role, String fieldName) {
        CitationLinkJpaEntity l = new CitationLinkJpaEntity();
        l.id = UUID.randomUUID();
        l.citationId = citationId;
        l.targetType = targetType;
        l.targetId = targetId;
        l.role = role;
        l.fieldName = fieldName;
        return l;
    }

    public UUID getId() { return id; }
    public UUID getCitationId() { return citationId; }
}
