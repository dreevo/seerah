package com.seerah.places.adapter.out.persistence;

import com.seerah.places.application.port.out.PlaceStore;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PlaceStoreAdapter implements PlaceStore {

    private static final String NAME_FIELD = "name";

    private final PlaceJpaRepository places;
    private final PlaceTranslationJpaRepository names;

    public PlaceStoreAdapter(PlaceJpaRepository places, PlaceTranslationJpaRepository names) {
        this.places = places;
        this.names = names;
    }

    @Override
    public UUID upsert(String slug, double latitude, double longitude, String modernName, boolean approximate) {
        PlaceJpaEntity p = places.findBySlug(slug).orElseGet(() -> PlaceJpaEntity.create(slug));
        p.setLatitude(latitude);
        p.setLongitude(longitude);
        p.setModernName(modernName);
        p.setApproximate(approximate);
        return places.save(p).getId();
    }

    @Override
    public void putName(UUID placeId, String locale, String value, String script) {
        names.findFirstByEntityTypeAndEntityIdAndFieldNameAndLocale(EntityType.PLACE, placeId, NAME_FIELD, locale)
                .ifPresentOrElse(
                        existing -> existing.setValue(value),
                        () -> names.save(PlaceTranslationJpaEntity.create(placeId, NAME_FIELD, locale, value)));
    }

    @Override
    public Optional<PlaceData> findById(UUID id, String locale) {
        return places.findById(id).map(p -> toData(p, locale));
    }

    @Override
    public Optional<PlaceData> findBySlug(String slug, String locale) {
        return places.findBySlug(slug).map(p -> toData(p, locale));
    }

    @Override
    public List<PlaceData> findAll(String locale) {
        return places.findAll().stream().map(p -> toData(p, locale)).toList();
    }

    private PlaceData toData(PlaceJpaEntity p, String locale) {
        String name = name(p.getId(), locale).or(() -> name(p.getId(), "en")).orElse(p.getSlug());
        return new PlaceData(p.getId(), p.getSlug(), name, p.getModernName(),
                p.getLatitude(), p.getLongitude(), p.isApproximate());
    }

    private Optional<String> name(UUID placeId, String locale) {
        return names.findFirstByEntityTypeAndEntityIdAndFieldNameAndLocale(EntityType.PLACE, placeId, NAME_FIELD, locale)
                .map(PlaceTranslationJpaEntity::getValue);
    }
}
