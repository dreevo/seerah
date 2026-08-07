package com.seerah.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChronicleJpaRepository extends JpaRepository<ChronicleJpaEntity, UUID> {

    List<ChronicleJpaEntity> findAllByOrderByOrdinalAscTitleAsc();

    Optional<ChronicleJpaEntity> findBySlug(String slug);
}
