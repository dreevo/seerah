package com.seerah.content.adapter.out.persistence;

import com.seerah.content.application.port.out.ContentTextPort;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Generic localisable-text access over the {@code translation} table (§11.2). */
@Component
public class ContentTextAdapter implements ContentTextPort {

    private final TranslationJpaRepository repository;

    public ContentTextAdapter(TranslationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void putText(EntityType type, UUID id, String field, String locale, String value) {
        repository.findByEntityTypeAndEntityIdAndFieldNameAndLocale(type, id, field, locale)
                .ifPresentOrElse(
                        existing -> existing.setValue(value),
                        () -> repository.save(TranslationJpaEntity.create(type, id, field, locale, value)));
    }

    @Override
    public Optional<String> text(EntityType type, UUID id, String field, String locale) {
        return repository.findByEntityTypeAndEntityIdAndFieldNameAndLocale(type, id, field, locale)
                .map(TranslationJpaEntity::getValue);
    }
}
