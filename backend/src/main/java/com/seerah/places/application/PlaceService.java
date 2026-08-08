package com.seerah.places.application;

import com.seerah.places.api.PlaceReadPort;
import com.seerah.places.api.PlaceRegistrar;
import com.seerah.places.api.PlaceView;
import com.seerah.places.application.port.out.PlaceStore;
import com.seerah.shared.ScriptKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application service for places: reference ingestion and map reads. */
@Service
@Transactional
public class PlaceService implements PlaceReadPort, PlaceRegistrar {

    private final PlaceStore store;

    public PlaceService(PlaceStore store) {
        this.store = store;
    }

    @Override
    public UUID upsertPlace(Command c) {
        UUID id = store.upsert(c.slug(), c.latitude(), c.longitude(), c.modernName(), c.approximate());
        store.putName(id, "en", c.name(), ScriptKind.LATIN.name());
        if (c.nameArabic() != null && !c.nameArabic().isBlank()) {
            store.putName(id, "ar", c.nameArabic(), ScriptKind.ARABIC.name());
        }
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlaceView> findById(UUID id, String locale) {
        return store.findById(id, locale).map(PlaceService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlaceView> findBySlug(String slug, String locale) {
        return store.findBySlug(slug, locale).map(PlaceService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceView> publishedList(String locale) {
        return store.findAll(locale).stream().map(PlaceService::toView).toList();
    }

    private static PlaceView toView(PlaceStore.PlaceData d) {
        return new PlaceView(d.id(), d.slug(), d.name(), d.modernName(),
                d.latitude(), d.longitude(), d.approximate());
    }
}
