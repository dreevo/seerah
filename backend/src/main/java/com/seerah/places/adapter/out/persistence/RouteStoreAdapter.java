package com.seerah.places.adapter.out.persistence;

import com.seerah.places.api.GeoPoint;
import com.seerah.places.application.port.out.RouteStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Route store backed by plain tables: a {@code route} row plus its ordered
 * {@code route_point}s. Distance is the great-circle (haversine) length of the
 * polyline, computed in Java — no PostGIS. Upsert is delete-then-insert on the
 * unique slug, which keeps re-seeding idempotent.
 */
@Component
public class RouteStoreAdapter implements RouteStore {

    private static final double EARTH_KM = 6371.0088;

    @PersistenceContext
    private EntityManager em;

    @Override
    public UUID upsert(String slug, UUID eventId, boolean conjectural, List<GeoPoint> points) {
        em.createNativeQuery("DELETE FROM route WHERE slug = :slug")
                .setParameter("slug", slug).executeUpdate();

        UUID id = UUID.randomUUID();
        BigDecimal km = BigDecimal.valueOf(lengthKm(points)).setScale(2, RoundingMode.HALF_UP);
        em.createNativeQuery("""
                INSERT INTO route (id, slug, event_id, is_conjectural, distance_km)
                VALUES (:id, :slug, :event, :conj, :km)
                """)
                .setParameter("id", id)
                .setParameter("slug", slug)
                .setParameter("event", eventId)
                .setParameter("conj", conjectural)
                .setParameter("km", km)
                .executeUpdate();

        for (int i = 0; i < points.size(); i++) {
            GeoPoint p = points.get(i);
            em.createNativeQuery("""
                    INSERT INTO route_point (route_id, ordinal, lat, lng)
                    VALUES (:rid, :ord, :lat, :lng)
                    """)
                    .setParameter("rid", id)
                    .setParameter("ord", i)
                    .setParameter("lat", p.lat())
                    .setParameter("lng", p.lng())
                    .executeUpdate();
        }
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RouteData> forEvent(UUID eventId) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, slug, is_conjectural, distance_km
                FROM route WHERE event_id = :eid ORDER BY slug
                """)
                .setParameter("eid", eventId)
                .getResultList();

        List<RouteData> out = new ArrayList<>();
        for (Object[] r : rows) {
            UUID routeId = (UUID) r[0];
            Double km = r[3] == null ? null : ((BigDecimal) r[3]).doubleValue();
            out.add(new RouteData(r[1].toString(), (Boolean) r[2], km, pointsOf(routeId)));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<GeoPoint> pointsOf(UUID routeId) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT lat, lng FROM route_point WHERE route_id = :rid ORDER BY ordinal
                """)
                .setParameter("rid", routeId)
                .getResultList();
        List<GeoPoint> pts = new ArrayList<>();
        for (Object[] r : rows) {
            pts.add(new GeoPoint(((Number) r[0]).doubleValue(), ((Number) r[1]).doubleValue()));
        }
        return pts;
    }

    /** Great-circle length of the polyline in kilometres. */
    private static double lengthKm(List<GeoPoint> pts) {
        double total = 0;
        for (int i = 1; i < pts.size(); i++) {
            total += haversineKm(pts.get(i - 1), pts.get(i));
        }
        return total;
    }

    private static double haversineKm(GeoPoint a, GeoPoint b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLng = Math.toRadians(b.lng() - a.lng());
        double la1 = Math.toRadians(a.lat());
        double la2 = Math.toRadians(b.lat());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(la1) * Math.cos(la2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
