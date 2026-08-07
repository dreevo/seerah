package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, UUID> {

    Optional<EventJpaEntity> findBySlug(String slug);

    List<EventJpaEntity> findByStatusOrderBySortKeyAscGregStartAscIdAsc(ContentStatus status);

    List<EventJpaEntity> findByStatusAndChronicleIdOrderBySortKeyAscGregStartAscIdAsc(
            ContentStatus status, UUID chronicleId);

    long countByStatusAndChronicleId(ContentStatus status, UUID chronicleId);
}
