package com.seerah.places.adapter.out.persistence;

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
 * The {@code places} module's view of the shared {@code translation} table, used to
 * store a place's localised name (§11.2). A second JPA entity over the same table
 * as content's; they never touch the same rows (distinct {@code entity_type}).
 */
@Entity(name = "PlaceTranslation")
@Table(name = "translation")
public class PlaceTranslationJpaEntity {

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
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected PlaceTranslationJpaEntity() { }

    public static PlaceTranslationJpaEntity create(UUID placeId, String field, String locale, String value) {
        PlaceTranslationJpaEntity t = new PlaceTranslationJpaEntity();
        t.id = UUID.randomUUID();
        t.entityType = EntityType.PLACE;
        t.entityId = placeId;
        t.fieldName = field;
        t.locale = locale;
        t.value = value;
        return t;
    }

    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; this.updatedAt = Instant.now(); }
}
