package com.seerah.provenance.application.port.out;

import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import com.seerah.shared.HadithGrade;
import com.seerah.shared.SourceTier;

import java.util.List;
import java.util.UUID;

/**
 * The single outbound port through which the provenance application reaches its
 * store. Keeps the application free of JPA (§23.1); the persistence adapter is
 * the only class that knows about tables.
 */
public interface ProvenanceStore {

    UUID saveSource(String slug, String workTitle, String author, SourceTier tier, boolean quotable);

    UUID saveCitation(UUID sourceId, String locator, String locatorKind, String quote, HadithGrade grade);

    UUID saveLink(UUID citationId, EntityType targetType, UUID targetId, CitationRole role, String fieldName);

    UUID savePosition(EntityType targetType, UUID targetId, String positionKey, String heldBy,
                      String summary, UUID citationId, int ordinal);

    long countSupportingLinks(EntityType targetType, UUID targetId);

    int countPositions(EntityType targetType, UUID targetId);

    record CitationDetail(String workTitle, String tier, String locator, boolean quotable, String quote, String grade) { }

    List<CitationDetail> listCitationsFor(EntityType targetType, UUID targetId);
}
