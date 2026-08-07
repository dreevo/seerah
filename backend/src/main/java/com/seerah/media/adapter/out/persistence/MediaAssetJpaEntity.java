package com.seerah.media.adapter.out.persistence;

import com.seerah.media.domain.MediaKind;
import com.seerah.shared.ContentStatus;
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

/** A media asset (§12.9). Never a depiction of a person — see {@link MediaKind}. */
@Entity
@Table(name = "media_asset")
public class MediaAssetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "media_kind")
    private MediaKind kind;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "checksum_sha256", nullable = false)
    private byte[] checksumSha256;

    @Column(nullable = false)
    private String licence;

    @Column(nullable = false)
    private String attribution;

    @Column(name = "source_url")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MediaAssetJpaEntity() { }

    public static MediaAssetJpaEntity create(String s3Key, MediaKind kind, String mimeType, long byteSize,
                                             byte[] checksum, String licence, String attribution, String sourceUrl) {
        MediaAssetJpaEntity m = new MediaAssetJpaEntity();
        m.id = UUID.randomUUID();
        m.s3Key = s3Key;
        m.kind = kind;
        m.mimeType = mimeType;
        m.byteSize = byteSize;
        m.checksumSha256 = checksum;
        m.licence = licence;
        m.attribution = attribution;
        m.sourceUrl = sourceUrl;
        return m;
    }

    public UUID getId() { return id; }
    public String getS3Key() { return s3Key; }
    public MediaKind getKind() { return kind; }
    public String getLicence() { return licence; }
    public String getAttribution() { return attribution; }
    public String getSourceUrl() { return sourceUrl; }
}
