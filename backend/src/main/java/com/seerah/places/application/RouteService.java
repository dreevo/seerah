package com.seerah.places.application;

import com.seerah.places.api.GeoPoint;
import com.seerah.places.api.RouteReadPort;
import com.seerah.places.api.RouteRegistrar;
import com.seerah.places.api.RouteView;
import com.seerah.places.application.port.out.RouteStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Application service for routes: ingestion and map reads. */
@Service
@Transactional
public class RouteService implements RouteReadPort, RouteRegistrar {

    private final RouteStore store;

    public RouteService(RouteStore store) {
        this.store = store;
    }

    @Override
    public UUID upsertRoute(String slug, UUID eventId, boolean conjectural, List<GeoPoint> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("a route needs at least two points");
        }
        return store.upsert(slug, eventId, conjectural, points);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteView> routesForEvent(UUID eventId) {
        return store.forEvent(eventId).stream()
                .map(r -> new RouteView(r.slug(), r.conjectural(), r.distanceKm(), r.points()))
                .toList();
    }
}
