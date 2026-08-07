package com.seerah.content.adapter.out.persistence;

import com.seerah.content.application.port.out.EventTranslationPort;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Persists and reads an event's localisable strings via the {@code translation} table. */
@Component
public class EventTranslationAdapter implements EventTranslationPort {

    private final TranslationJpaRepository repository;

    public EventTranslationAdapter(TranslationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void putValue(UUID eventId, String fieldName, String locale, String value) {
        repository.findByEntityTypeAndEntityIdAndFieldNameAndLocale(
                        EntityType.EVENT, eventId, fieldName, locale)
                .ifPresentOrElse(
                        existing -> existing.setValue(value),
                        () -> repository.save(TranslationJpaEntity.create(
                                EntityType.EVENT, eventId, fieldName, locale, value)));
    }

    @Override
    public Optional<String> value(UUID eventId, String fieldName, String locale) {
        return repository.findByEntityTypeAndEntityIdAndFieldNameAndLocale(
                        EntityType.EVENT, eventId, fieldName, locale)
                .map(TranslationJpaEntity::getValue);
    }
}
