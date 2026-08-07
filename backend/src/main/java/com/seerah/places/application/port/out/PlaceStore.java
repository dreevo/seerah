package com.seerah.places.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound store for places and their localised names. */
public interface PlaceStore {

    UUID upsert(String slug, double latitude, double longitude, String modernName, boolean approximate);

    void putName(UUID placeId, String locale, String value, String script);

    record PlaceData(UUID id, String slug, String name, String modernName,
                     Double latitude, Double longitude, boolean approximate) { }

    Optional<PlaceData> findById(UUID id, String locale);

    Optional<PlaceData> findBySlug(String slug, String locale);
}
