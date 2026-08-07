package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.util.UUID;

/** One ordered step of a learning path, pointing at an entity (an event in Phase 1). */
@Entity
@Table(name = "learning_path_step")
public class LearningPathStepJpaEntity {

    @Id
    private UUID id;

    @Column(name = "path_id", nullable = false)
    private UUID pathId;

    @Column(nullable = false)
    private int ordinal;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "target_type", nullable = false, columnDefinition = "entity_type")
    private EntityType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    private String prompt;

    protected LearningPathStepJpaEntity() { }

    public static LearningPathStepJpaEntity event(UUID pathId, int ordinal, UUID eventId, String prompt) {
        LearningPathStepJpaEntity s = new LearningPathStepJpaEntity();
        s.id = UUID.randomUUID();
        s.pathId = pathId;
        s.ordinal = ordinal;
        s.targetType = EntityType.EVENT;
        s.targetId = eventId;
        s.prompt = prompt;
        return s;
    }

    public int getOrdinal() { return ordinal; }
    public EntityType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getPrompt() { return prompt; }
}
