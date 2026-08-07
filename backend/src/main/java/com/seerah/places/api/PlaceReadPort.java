package com.seerah.places.api;

import java.util.Optional;
import java.util.UUID;

/** Published read contract of the {@code places} module. */
public interface PlaceReadPort {

    Optional<PlaceView> findById(UUID id, String locale);

    Optional<PlaceView> findBySlug(String slug, String locale);
}
