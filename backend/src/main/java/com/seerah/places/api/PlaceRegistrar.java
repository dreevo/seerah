package com.seerah.places.api;

import java.util.UUID;

/** Ingestion contract for places (reference geography). */
public interface PlaceRegistrar {

    UUID upsertPlace(Command command);

    record Command(
            String slug,
            String name,
            String nameArabic,
            String modernName,
            double latitude,
            double longitude,
            boolean approximate) {
    }
}
