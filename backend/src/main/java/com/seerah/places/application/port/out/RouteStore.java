package com.seerah.places.application.port.out;

import com.seerah.places.api.GeoPoint;

import java.util.List;
import java.util.UUID;

/** Outbound store for routes (an ordered list of lat/lng points per route). */
public interface RouteStore {

    UUID upsert(String slug, UUID eventId, boolean conjectural, List<GeoPoint> points);

    record RouteData(String slug, boolean conjectural, Double distanceKm, List<GeoPoint> points) { }

    List<RouteData> forEvent(UUID eventId);
}
