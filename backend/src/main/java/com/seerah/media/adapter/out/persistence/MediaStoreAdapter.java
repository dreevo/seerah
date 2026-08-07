package com.seerah.media.adapter.out.persistence;

import com.seerah.media.application.port.out.MediaStore;
import com.seerah.media.domain.MediaKind;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MediaStoreAdapter implements MediaStore {

    private final MediaAssetJpaRepository assets;
    private final MediaLinkJpaRepository links;

    MediaStoreAdapter(MediaAssetJpaRepository assets, MediaLinkJpaRepository links) {
        this.assets = assets;
        this.links = links;
    }

    @Override
    public UUID saveAsset(String s3Key, MediaKind kind, String mimeType, long byteSize,
                          String licence, String attribution, String sourceUrl) {
        byte[] checksum = sha256(s3Key);
        return assets.save(MediaAssetJpaEntity.create(
                s3Key, kind, mimeType, byteSize, checksum, licence, attribution, sourceUrl)).getId();
    }

    @Override
    public void link(UUID mediaId, UUID eventId, int ordinal, String caption) {
        links.save(MediaLinkJpaEntity.forEvent(mediaId, eventId, ordinal, caption));
    }

    @Override
    public List<MediaRow> forEvent(UUID eventId) {
        List<MediaRow> out = new ArrayList<>();
        for (MediaLinkJpaEntity link : links.findByTargetTypeAndTargetIdOrderByOrdinalAsc(EntityType.EVENT, eventId)) {
            assets.findById(link.getMediaId()).ifPresent(a ->
                    out.add(new MediaRow(a.getKind().name(), link.getCaption(),
                            a.getAttribution(), a.getLicence(), a.getSourceUrl(), a.getS3Key())));
        }
        return out;
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
