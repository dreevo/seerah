package com.seerah.content.adapter.out.persistence;

import com.seerah.content.domain.CalendarSystem;
import com.seerah.content.domain.DatePrecision;
import com.seerah.shared.Certainty;
import com.seerah.shared.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The persistence model for {@code event} (§23.3 — deliberately separate from the
 * {@link com.seerah.content.domain.Event} domain object, so the schema can evolve
 * without dragging the domain with it). Native Postgres enums are bound via
 * {@link PostgreSQLEnumJdbcType}.
 */
@Entity
@Table(name = "event")
public class EventJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String slug;

    @Column(name = "chronicle_id")
    private UUID chronicleId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "calendar_system")
    private CalendarSystem calendar;

    @Column(name = "hijri_year")
    private Integer hijriYear;

    @Column(name = "hijri_month")
    private Short hijriMonth;

    @Column(name = "hijri_day")
    private Short hijriDay;

    @Column(name = "greg_start")
    private LocalDate gregStart;

    @Column(name = "greg_end")
    private LocalDate gregEnd;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "date_precision", nullable = false, columnDefinition = "date_precision")
    private DatePrecision datePrecision;

    @Column(name = "date_note")
    private String dateNote;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "certainty")
    private Certainty certainty;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status;

    @Column(name = "is_major", nullable = false)
    private boolean major;

    @Column(name = "sort_key", nullable = false)
    private int sortKey;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected EventJpaEntity() { }

    // --- getters / setters (persistence plumbing only) -----------------------
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public UUID getChronicleId() { return chronicleId; }
    public void setChronicleId(UUID chronicleId) { this.chronicleId = chronicleId; }
    public CalendarSystem getCalendar() { return calendar; }
    public void setCalendar(CalendarSystem calendar) { this.calendar = calendar; }
    public Integer getHijriYear() { return hijriYear; }
    public void setHijriYear(Integer hijriYear) { this.hijriYear = hijriYear; }
    public Short getHijriMonth() { return hijriMonth; }
    public void setHijriMonth(Short hijriMonth) { this.hijriMonth = hijriMonth; }
    public Short getHijriDay() { return hijriDay; }
    public void setHijriDay(Short hijriDay) { this.hijriDay = hijriDay; }
    public LocalDate getGregStart() { return gregStart; }
    public void setGregStart(LocalDate gregStart) { this.gregStart = gregStart; }
    public LocalDate getGregEnd() { return gregEnd; }
    public void setGregEnd(LocalDate gregEnd) { this.gregEnd = gregEnd; }
    public DatePrecision getDatePrecision() { return datePrecision; }
    public void setDatePrecision(DatePrecision datePrecision) { this.datePrecision = datePrecision; }
    public String getDateNote() { return dateNote; }
    public void setDateNote(String dateNote) { this.dateNote = dateNote; }
    public Certainty getCertainty() { return certainty; }
    public void setCertainty(Certainty certainty) { this.certainty = certainty; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public boolean isMajor() { return major; }
    public void setMajor(boolean major) { this.major = major; }
    public int getSortKey() { return sortKey; }
    public void setSortKey(int sortKey) { this.sortKey = sortKey; }
    public long getVersion() { return version; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
