package com.seerah.people.adapter.out.persistence;

import com.seerah.shared.ScriptKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/** An alternate name / transliteration for a person (§12.4). Feeds search recall. */
@Entity
@Table(name = "person_alias")
public class PersonAliasJpaEntity {

    @Id
    private UUID id;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(nullable = false)
    private String alias;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "script_kind")
    private ScriptKind script;

    private String locale;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PersonAliasJpaEntity() { }

    public static PersonAliasJpaEntity create(UUID personId, String alias, ScriptKind script, boolean primary) {
        PersonAliasJpaEntity a = new PersonAliasJpaEntity();
        a.id = UUID.randomUUID();
        a.personId = personId;
        a.alias = alias;
        a.script = script;
        a.primary = primary;
        return a;
    }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}
