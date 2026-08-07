package com.seerah.scripture.adapter.out.persistence;

import com.seerah.scripture.domain.RevelationPlace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/** Reference data: one row per surah (§12.7). Not editorial content. */
@Entity
@Table(name = "surah")
public class SurahJpaEntity {

    @Id
    private Short number;

    @Column(name = "name_ar", nullable = false) private String nameAr;
    @Column(name = "name_translit", nullable = false) private String nameTranslit;
    @Column(name = "name_en", nullable = false) private String nameEn;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "revelation_place", nullable = false, columnDefinition = "revelation_place")
    private RevelationPlace revelationPlace;

    @Column(name = "revelation_order", nullable = false) private Short revelationOrder;
    @Column(name = "ayah_count", nullable = false) private Short ayahCount;
    @Column(name = "has_bismillah", nullable = false) private boolean hasBismillah = true;

    protected SurahJpaEntity() { }

    public SurahJpaEntity(Short number) { this.number = number; }

    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String v) { this.nameAr = v; }
    public void setNameTranslit(String v) { this.nameTranslit = v; }
    public void setNameEn(String v) { this.nameEn = v; }
    public void setRevelationPlace(RevelationPlace v) { this.revelationPlace = v; }
    public void setRevelationOrder(Short v) { this.revelationOrder = v; }
    public void setAyahCount(Short v) { this.ayahCount = v; }
}
