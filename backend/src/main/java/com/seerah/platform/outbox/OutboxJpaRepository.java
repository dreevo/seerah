package com.seerah.platform.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxRecord, Long> {

    /** The relay cursor: the oldest unprocessed outbox rows (§25.5, Phase-1 poller). */
    List<OutboxRecord> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
