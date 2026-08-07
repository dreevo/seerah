package com.seerah.provenance.adapter.out.persistence;

import com.seerah.provenance.application.port.out.ProvenanceStore;
import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import com.seerah.shared.HadithGrade;
import com.seerah.shared.SourceTier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** JPA-backed implementation of the provenance out-port (§23.1). */
@Component
public class ProvenanceStoreAdapter implements ProvenanceStore {

    /** Which citation roles count as evidence *for* a claim (§13.2). */
    private static final List<CitationRole> SUPPORTING =
            List.of(CitationRole.SUPPORTS, CitationRole.PRIMARY_FOR, CitationRole.DETAILS);

    private final SourceJpaRepository sources;
    private final CitationJpaRepository citations;
    private final CitationLinkJpaRepository links;
    private final ScholarlyPositionJpaRepository positions;

    public ProvenanceStoreAdapter(SourceJpaRepository sources, CitationJpaRepository citations,
                                  CitationLinkJpaRepository links, ScholarlyPositionJpaRepository positions) {
        this.sources = sources;
        this.citations = citations;
        this.links = links;
        this.positions = positions;
    }

    @Override
    public UUID saveSource(String slug, String workTitle, String author, SourceTier tier, boolean quotable) {
        return sources.save(SourceJpaEntity.create(slug, workTitle, author, tier, quotable)).getId();
    }

    @Override
    public UUID saveCitation(UUID sourceId, String locator, String locatorKind, String quote, HadithGrade grade) {
        // A citation is a reusable fact keyed by (source, locator, quote): reuse an
        // existing row when one matches (§ux_citation) so a passage that names several
        // prophets together can support several events, then link each separately.
        return citations.findMatch(sourceId, locator, quote)
                .map(CitationJpaEntity::getId)
                .orElseGet(() -> citations.save(
                        CitationJpaEntity.create(sourceId, locator, locatorKind, quote, grade)).getId());
    }

    @Override
    public UUID saveLink(UUID citationId, EntityType targetType, UUID targetId, CitationRole role, String fieldName) {
        return links.save(CitationLinkJpaEntity.create(citationId, targetType, targetId, role, fieldName)).getId();
    }

    @Override
    public UUID savePosition(EntityType targetType, UUID targetId, String positionKey, String heldBy,
                             String summary, UUID citationId, int ordinal) {
        return positions.save(ScholarlyPositionJpaEntity.create(
                targetType, targetId, positionKey, heldBy, summary, citationId, ordinal)).getId();
    }

    @Override
    public long countSupportingLinks(EntityType targetType, UUID targetId) {
        return links.countByTargetTypeAndTargetIdAndRoleIn(targetType, targetId, SUPPORTING);
    }

    @Override
    public int countPositions(EntityType targetType, UUID targetId) {
        return (int) positions.countByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public List<CitationDetail> listCitationsFor(EntityType targetType, UUID targetId) {
        return links.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(link -> citations.findById(link.getCitationId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> {
                    var src = sources.findById(c.getSourceId()).orElse(null);
                    boolean quotable = src != null && src.isQuotable();
                    return new CitationDetail(
                            src == null ? "(unknown work)" : src.getWorkTitle(),
                            src == null ? null : src.getTier().name(),
                            c.getLocator(),
                            quotable,
                            quotable ? c.getQuote() : null,
                            c.getGrade() == null ? null : c.getGrade().name());
                })
                .toList();
    }
}
