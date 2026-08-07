package com.seerah.people.adapter.out.persistence;

import com.seerah.people.application.port.out.PersonPorts.PersonNamePort;
import com.seerah.shared.ScriptKind;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Stores and reads person display names in {@code person_alias}. */
@Component
public class PersonNameAdapter implements PersonNamePort {

    private final PersonAliasJpaRepository repository;

    public PersonNameAdapter(PersonAliasJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void putName(UUID personId, String name, String script, boolean primary) {
        ScriptKind kind = ScriptKind.valueOf(script);
        repository.findFirstByPersonIdAndScript(personId, kind).ifPresentOrElse(
                existing -> existing.setAlias(name),
                () -> repository.save(PersonAliasJpaEntity.create(personId, name, kind, primary)));
    }

    @Override
    public Optional<String> primaryName(UUID personId, String script) {
        ScriptKind kind = ScriptKind.valueOf(script);
        return repository.findFirstByPersonIdAndScriptAndPrimaryTrue(personId, kind)
                .or(() -> repository.findFirstByPersonIdAndScript(personId, kind))
                .map(PersonAliasJpaEntity::getAlias);
    }
}
