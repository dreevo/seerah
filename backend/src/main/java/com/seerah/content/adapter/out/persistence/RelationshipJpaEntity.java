package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.Certainty;
import com.seerah.shared.ContentStatus;
import com.seerah.shared.EntityType;
import com.seerah.shared.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The polymorphic edge (§12.5). Referential integrity is enforced by DB trigger. */
@Entity
@Table(name = "relationship")
public class RelationshipJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "subject_type", nullable = false, columnDefinition = "entity_type")
    private EntityType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "rel_type", nullable = false, columnDefinition = "relationship_type")
    private RelationshipType relType;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "object_type", nullable = false, columnDefinition = "entity_type")
    private EntityType objectType;

    @Column(name = "object_id", nullable = false)
    private UUID objectId;

    private String qualifier;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "certainty")
    private Certainty certainty = Certainty.REPORTED;

    @Column(name = "is_interpretive", nullable = false)
    private boolean interpretive;

    @Column(nullable = false)
    private BigDecimal weight = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected RelationshipJpaEntity() { }

    public static RelationshipJpaEntity create(EntityType subjectType, UUID subjectId, RelationshipType relType,
                                               EntityType objectType, UUID objectId, double weight,
                                               boolean interpretive, String qualifier) {
        RelationshipJpaEntity r = new RelationshipJpaEntity();
        r.id = UUID.randomUUID();
        r.subjectType = subjectType;
        r.subjectId = subjectId;
        r.relType = relType;
        r.objectType = objectType;
        r.objectId = objectId;
        r.weight = BigDecimal.valueOf(weight);
        r.interpretive = interpretive;
        r.qualifier = qualifier;
        return r;
    }

    public UUID getId() { return id; }
    public EntityType getSubjectType() { return subjectType; }
    public UUID getSubjectId() { return subjectId; }
    public RelationshipType getRelType() { return relType; }
    public EntityType getObjectType() { return objectType; }
    public UUID getObjectId() { return objectId; }
    public BigDecimal getWeight() { return weight; }
    public boolean isInterpretive() { return interpretive; }
    public String getQualifier() { return qualifier; }
}
