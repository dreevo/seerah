package com.seerah.provenance.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SourceJpaRepository extends JpaRepository<SourceJpaEntity, UUID> {
    Optional<SourceJpaEntity> findBySlug(String slug);
}
