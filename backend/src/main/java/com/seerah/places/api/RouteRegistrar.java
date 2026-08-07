package com.seerah.places.api;

import java.util.List;
import java.util.UUID;

/** Ingestion contract for routes. */
public interface RouteRegistrar {

    UUID upsertRoute(String slug, UUID eventId, boolean conjectural, List<GeoPoint> points);
}
