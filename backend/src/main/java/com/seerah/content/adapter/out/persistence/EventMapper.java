package com.seerah.content.adapter.out.persistence;

import com.seerah.content.domain.Event;
import com.seerah.content.domain.EventId;
import com.seerah.content.domain.HistoricalDate;
import com.seerah.shared.Slug;

import java.time.Instant;

/** Translates between the {@link Event} domain aggregate and its JPA entity (§23.3). */
final class EventMapper {

    private EventMapper() { }

    static Event toDomain(EventJpaEntity e) {
        HistoricalDate date = new HistoricalDate(
                e.getCalendar(),
                e.getHijriYear(),
                e.getHijriMonth() == null ? null : e.getHijriMonth().intValue(),
                e.getHijriDay() == null ? null : e.getHijriDay().intValue(),
                e.getGregStart(),
                e.getGregEnd(),
                e.getDatePrecision(),
                e.getDateNote());
        return Event.rehydrate(
                EventId.of(e.getId()),
                new Slug(e.getSlug()),
                e.getChronicleId(),
                date,
                e.getCertainty(),
                e.getStatus(),
                e.isMajor(),
                e.getSortKey(),
                e.getPublishedAt(),
                e.getVersion());
    }

    static void applyToEntity(Event ev, EventJpaEntity e) {
        HistoricalDate d = ev.date();
        e.setId(ev.id().value());
        e.setSlug(ev.slug().value());
        e.setChronicleId(ev.chronicleId());
        e.setCalendar(d.calendar());
        e.setHijriYear(d.hijriYear());
        e.setHijriMonth(d.hijriMonth() == null ? null : d.hijriMonth().shortValue());
        e.setHijriDay(d.hijriDay() == null ? null : d.hijriDay().shortValue());
        e.setGregStart(d.gregStart());
        e.setGregEnd(d.gregEnd());
        e.setDatePrecision(d.precision());
        e.setDateNote(d.note());
        e.setCertainty(ev.certainty());
        e.setStatus(ev.status());
        e.setMajor(ev.isMajor());
        e.setSortKey(ev.sortKey());
        e.setPublishedAt(ev.publishedAt());
        e.setUpdatedAt(Instant.now());
    }
}
