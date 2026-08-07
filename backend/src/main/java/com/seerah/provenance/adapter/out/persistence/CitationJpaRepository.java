package com.seerah.provenance.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CitationJpaRepository extends JpaRepository<CitationJpaEntity, UUID> {

    /**
     * A citation is a reusable fact: one (source, locator, quote) may support many
     * targets — e.g. al-Anbiyāʾ 21:85-86 names Idrīs and Dhū al-Kifl together, and
     * both events cite it. This mirrors the {@code ux_citation} unique constraint so
     * {@code saveCitation} can find-or-create instead of colliding.
     */
    @Query("""
            select c from CitationJpaEntity c
            where c.sourceId = :sourceId and c.locator = :locator
              and (c.quote = :quote or (c.quote is null and :quote is null))
            """)
    Optional<CitationJpaEntity> findMatch(@Param("sourceId") UUID sourceId,
                                          @Param("locator") String locator,
                                          @Param("quote") String quote);
}
