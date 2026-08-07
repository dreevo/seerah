package com.seerah.places.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaceJpaRepository extends JpaRepository<PlaceJpaEntity, UUID> {
    Optional<PlaceJpaEntity> findBySlug(String slug);
}
