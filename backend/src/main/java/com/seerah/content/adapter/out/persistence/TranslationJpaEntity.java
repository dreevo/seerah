package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
import com.seerah.shared.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/**
 * A single localised string for any content entity (§11.2, §12.9). The
 * {@code content} module owns the translations of its own entities; the table
 * itself is shared infrastructure.
 */
@Entity
@Table(name = "translation")
public class TranslationJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "entity_type", nullable = false, columnDefinition = "entity_type")
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String value;

    @Column(name = "is_machine", nullable = false)
    private boolean machine;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TranslationJpaEntity() { }

    public static TranslationJpaEntity create(EntityType type, UUID entityId, String field,
                                              String locale, String value) {
        TranslationJpaEntity t = new TranslationJpaEntity();
        t.id = UUID.randomUUID();
        t.entityType = type;
        t.entityId = entityId;
        t.fieldName = field;
        t.locale = locale;
        t.value = value;
        t.machine = false;
        t.status = ContentStatus.DRAFT;
        return t;
    }

    public String getValue() { return value; }

    public void setValue(String value) {
        this.value = value;
        this.updatedAt = Instant.now();
    }
}
