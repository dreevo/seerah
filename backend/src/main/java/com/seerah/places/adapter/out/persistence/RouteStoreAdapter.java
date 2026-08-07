package com.seerah.places.adapter.out.persistence;

import com.seerah.places.api.GeoPoint;
import com.seerah.places.application.port.out.RouteStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * PostGIS-backed route store. The path is built as WGS 84 WKT and inserted via
 * {@code ST_GeogFromText}; the distance is measured with {@code ST_Length} at
 * insert time. Reads come back as {@code ST_AsText} and are parsed into points —
 * so the JPA layer stays free of spatial types.
 */
@Component
public class RouteStoreAdapter implements RouteStore {

    @PersistenceContext
    private EntityManager em;

    @Override
    public UUID upsert(String slug, UUID eventId, boolean conjectural, List<GeoPoint> points) {
        String wkt = toWkt(points);
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO route (id, slug, event_id, is_conjectural, path, distance_km)
                VALUES (:id, :slug, :event, :conj,
                        ST_GeogFromText(:wkt),
                        round(ST_Length(ST_GeogFromText(:wkt))::numeric / 1000.0, 2))
                ON CONFLICT (slug) DO UPDATE SET
                    event_id       = EXCLUDED.event_id,
                    is_conjectural = EXCLUDED.is_conjectural,
                    path           = EXCLUDED.path,
                    distance_km    = EXCLUDED.distance_km,
                    updated_at     = now()
                """)
                .setParameter("id", id)
                .setParameter("slug", slug)
                .setParameter("event", eventId)
                .setParameter("conj", conjectural)
                .setParameter("wkt", wkt)
                .executeUpdate();
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RouteData> forEvent(UUID eventId) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT slug, is_conjectural, distance_km, ST_AsText(path::geometry)
                FROM route WHERE event_id = :eid ORDER BY slug
                """)
                .setParameter("eid", eventId)
                .getResultList();
        List<RouteData> out = new ArrayList<>();
        for (Object[] r : rows) {
            Double km = r[2] == null ? null : ((BigDecimal) r[2]).doubleValue();
            out.add(new RouteData(r[0].toString(), (Boolean) r[1], km, parseLineString(r[3].toString())));
        }
        return out;
    }

    private static String toWkt(List<GeoPoint> points) {
        StringJoiner coords = new StringJoiner(",");
        for (GeoPoint p : points) {
            coords.add(p.lng() + " " + p.lat()); // WKT is lon lat
        }
        return "SRID=4326;LINESTRING(" + coords + ")";
    }

    /** Parse "LINESTRING(lng lat,lng lat,…)" into points (lat/lng order flipped back). */
    private static List<GeoPoint> parseLineString(String wkt) {
        List<GeoPoint> pts = new ArrayList<>();
        int open = wkt.indexOf('(');
        int close = wkt.lastIndexOf(')');
        if (open < 0 || close < 0) return pts;
        for (String pair : wkt.substring(open + 1, close).split(",")) {
            String[] lonlat = pair.trim().split("\\s+");
            if (lonlat.length == 2) {
                pts.add(new GeoPoint(Double.parseDouble(lonlat[1]), Double.parseDouble(lonlat[0])));
            }
        }
        return pts;
    }
}
