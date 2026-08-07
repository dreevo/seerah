package com.seerah.people.adapter.out.persistence;

import com.seerah.people.domain.PersonRole;
import com.seerah.shared.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "person")
public class PersonJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String slug;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "role_type", nullable = false, columnDefinition = "person_role")
    private PersonRole roleType;

    private String kunya;
    private String nasab;

    @Column(name = "birth_year_ce") private Integer birthYearCe;
    @Column(name = "death_year_ce") private Integer deathYearCe;
    @Column(name = "birth_year_ah") private Integer birthYearAh;
    @Column(name = "death_year_ah") private Integer deathYearAh;
    @Column(name = "honorific_key") private String honorificKey;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected PersonJpaEntity() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public PersonRole getRoleType() { return roleType; }
    public void setRoleType(PersonRole roleType) { this.roleType = roleType; }
    public Integer getBirthYearCe() { return birthYearCe; }
    public void setBirthYearCe(Integer v) { this.birthYearCe = v; }
    public Integer getDeathYearCe() { return deathYearCe; }
    public void setDeathYearCe(Integer v) { this.deathYearCe = v; }
    public Integer getBirthYearAh() { return birthYearAh; }
    public void setBirthYearAh(Integer v) { this.birthYearAh = v; }
    public Integer getDeathYearAh() { return deathYearAh; }
    public void setDeathYearAh(Integer v) { this.deathYearAh = v; }
    public String getHonorificKey() { return honorificKey; }
    public void setHonorificKey(String v) { this.honorificKey = v; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public long getVersion() { return version; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant v) { this.publishedAt = v; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
