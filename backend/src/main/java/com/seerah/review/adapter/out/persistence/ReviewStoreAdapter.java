package com.seerah.review.adapter.out.persistence;

import com.seerah.review.application.port.out.ReviewStore;
import com.seerah.shared.EntityType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JPA/native implementation of the review store. The approval insert computes the
 * content hash in SQL via {@code fn_content_hash} so it is identical to what the
 * publish trigger recomputes (§13.6) — the two never disagree because there is
 * only one implementation, in the database.
 */
@Component
public class ReviewStoreAdapter implements ReviewStore {

    @PersistenceContext
    private EntityManager em;

    @Override
    public UUID ensureScholar(String email, String displayName) {
        Object id = em.createNativeQuery("""
                INSERT INTO app_user (id, email, display_name)
                VALUES (:id, :email, :name)
                ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name
                RETURNING id
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("email", email)
                .setParameter("name", displayName)
                .getSingleResult();
        return toUuid(id);
    }

    @Override
    public void recordApproval(EntityType targetType, UUID targetId, int version, UUID scholarId, String note) {
        em.createNativeQuery("""
                INSERT INTO approval (id, target_type, target_id, target_version, scholar_id, content_hash, scope, note)
                VALUES (:id, CAST(:type AS entity_type), :tid, :ver, :sid,
                        fn_content_hash(CAST(:type AS entity_type), :tid), 'FULL', :note)
                ON CONFLICT (target_type, target_id, scholar_id)
                DO UPDATE SET target_version = EXCLUDED.target_version,
                              content_hash   = EXCLUDED.content_hash,
                              note           = EXCLUDED.note,
                              created_at     = now()
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("type", targetType.name())
                .setParameter("tid", targetId)
                .setParameter("ver", version)
                .setParameter("sid", scholarId)
                .setParameter("note", note)
                .executeUpdate();
    }

    @Override
    public void recordAction(EntityType targetType, UUID targetId, int version, String decision,
                             String fromStatus, String toStatus, UUID actorId, String comment) {
        em.createNativeQuery("""
                INSERT INTO review_action (id, target_type, target_id, target_version, decision,
                                           from_status, to_status, actor_id, comment)
                VALUES (:id, CAST(:type AS entity_type), :tid, :ver, CAST(:dec AS review_decision),
                        CAST(:from AS content_status), CAST(:to AS content_status), :actor, :comment)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("type", targetType.name())
                .setParameter("tid", targetId)
                .setParameter("ver", version)
                .setParameter("dec", decision)
                .setParameter("from", fromStatus)
                .setParameter("to", toStatus)
                .setParameter("actor", actorId)
                .setParameter("comment", comment)
                .executeUpdate();
    }

    @Override
    public int countApprovals(EntityType targetType, UUID targetId) {
        Object n = em.createNativeQuery("""
                SELECT count(*) FROM approval
                WHERE target_type = CAST(:type AS entity_type) AND target_id = :tid
                """)
                .setParameter("type", targetType.name())
                .setParameter("tid", targetId)
                .getSingleResult();
        return ((Number) n).intValue();
    }

    private static UUID toUuid(Object o) {
        return o instanceof UUID u ? u : UUID.fromString(o.toString());
    }
}
