package com.seerah.media.api;

import com.seerah.media.domain.MediaKind;

import java.util.UUID;

/** Ingestion contract for media assets and their placement against content. */
public interface MediaRegistrar {

    /** Register an asset. Attribution is mandatory — the platform will not hold an unattributed asset. */
    UUID registerAsset(RegisterAsset command);

    /** Place an asset beside an event with an optional caption. */
    void linkToEvent(UUID mediaId, UUID eventId, int ordinal, String caption);

    record RegisterAsset(String s3Key, MediaKind kind, String mimeType, long byteSize,
                         String licence, String attribution, String sourceUrl) {
    }
}
