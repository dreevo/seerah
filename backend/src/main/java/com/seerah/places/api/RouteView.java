package com.seerah.places.api;

import java.util.List;

/** A journey path for the map: an ordered set of points, its length, and whether it is conjectural. */
public record RouteView(
        String slug,
        boolean conjectural,
        Double distanceKm,
        List<GeoPoint> points) {
}
