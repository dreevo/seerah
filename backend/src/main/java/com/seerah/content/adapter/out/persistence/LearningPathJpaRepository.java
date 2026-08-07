package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningPathJpaRepository extends JpaRepository<LearningPathJpaEntity, UUID> {

    List<LearningPathJpaEntity> findByStatusOrderBySlugAsc(ContentStatus status);

    Optional<LearningPathJpaEntity> findBySlug(String slug);
}
