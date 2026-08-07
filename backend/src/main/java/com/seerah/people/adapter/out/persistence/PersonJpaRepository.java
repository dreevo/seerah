package com.seerah.people.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonJpaRepository extends JpaRepository<PersonJpaEntity, UUID> {
    Optional<PersonJpaEntity> findBySlug(String slug);

    List<PersonJpaEntity> findByStatusOrderBySlugAsc(ContentStatus status);
}
