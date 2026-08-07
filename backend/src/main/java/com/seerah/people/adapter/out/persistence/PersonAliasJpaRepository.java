package com.seerah.people.adapter.out.persistence;

import com.seerah.shared.ScriptKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonAliasJpaRepository extends JpaRepository<PersonAliasJpaEntity, UUID> {

    Optional<PersonAliasJpaEntity> findFirstByPersonIdAndScriptAndPrimaryTrue(UUID personId, ScriptKind script);

    Optional<PersonAliasJpaEntity> findFirstByPersonIdAndScript(UUID personId, ScriptKind script);
}
