package com.seerah.media.adapter.out.persistence;

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

/** Placement of an asset against a content entity. */
@Entity
@Table(name = "media_link")
public class MediaLinkJpaEntity {

    @Id
    private UUID id;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "target_type", nullable = false, columnDefinition = "entity_type")
    private EntityType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private int ordinal;

    private String caption;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MediaLinkJpaEntity() { }

    public static MediaLinkJpaEntity forEvent(UUID mediaId, UUID eventId, int ordinal, String caption) {
        MediaLinkJpaEntity l = new MediaLinkJpaEntity();
        l.id = UUID.randomUUID();
        l.mediaId = mediaId;
        l.targetType = EntityType.EVENT;
        l.targetId = eventId;
        l.ordinal = ordinal;
        l.caption = caption;
        return l;
    }

    public UUID getMediaId() { return mediaId; }
    public String getCaption() { return caption; }
}
