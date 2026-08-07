package com.seerah.media.application.port.out;

import com.seerah.media.domain.MediaKind;

import java.util.List;
import java.util.UUID;

/** Outbound store for media assets and their placements. */
public interface MediaStore {

    UUID saveAsset(String s3Key, MediaKind kind, String mimeType, long byteSize,
                   String licence, String attribution, String sourceUrl);

    void link(UUID mediaId, UUID eventId, int ordinal, String caption);

    record MediaRow(String kind, String caption, String attribution, String licence,
                    String sourceUrl, String s3Key) { }

    List<MediaRow> forEvent(UUID eventId);
}
