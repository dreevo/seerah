package com.seerah.media.api;

/**
 * A media asset as shown beside an event. Never a person: {@code kind} is drawn
 * from {@link com.seerah.media.domain.MediaKind}, which cannot express a depiction
 * of one (§6.5). {@code attribution} is always present — the schema forbids an
 * asset without it (§12.9).
 */
public record MediaView(
        String kind,
        String caption,
        String attribution,
        String licence,
        String sourceUrl,
        String s3Key) {
}
