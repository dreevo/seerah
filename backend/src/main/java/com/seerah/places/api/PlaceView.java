package com.seerah.places.api;

import java.util.UUID;

/**
 * A place as shown on the map. {@code approximate} is true when the location is
 * scholarly conjecture — the reader renders a shaded radius rather than a pin, and
 * claiming precision we do not have is treated as a scholarly fault (§12.4).
 */
public record PlaceView(
        UUID id,
        String slug,
        String name,
        String modernName,
        Double latitude,
        Double longitude,
        boolean approximate) {
}
