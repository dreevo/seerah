package com.seerah.publicapi;

import java.util.List;
import java.util.UUID;

/** The response shapes served to the public reader (the Angular app). */
public final class PublicViews {

    private PublicViews() { }

    public record ChronicleItem(String slug, String title, String titleAr, String subtitle,
                                String blurb, String glyph, String kind, int ordinal, int eventCount) { }

    public record TimelineItem(UUID id, String slug, String title,
                               Integer hijriYear, Integer gregYear, String certainty, boolean major) { }

    public record RelatedPerson(UUID id, String slug, String name, String nameArabic,
                                String role, String relation) { }

    public record RelatedVerse(String reference, String surahNameEn, String surahNameAr,
                               String textUthmani, String translation, String translator, String relation) { }

    public record RelatedEventItem(UUID id, String slug, String title, String relation) { }

    public record RelatedPlace(UUID id, String slug, String name, String modernName,
                               Double latitude, Double longitude, boolean approximate, String relation) { }

    public record MapPoint(double lat, double lng) { }

    public record RouteLine(String slug, boolean conjectural, Double distanceKm, List<MapPoint> points) { }

    public record MediaItem(String kind, String caption, String attribution, String licence, String sourceUrl) { }

    public record SourceItem(String workTitle, String tier, String locator, String quote, String grade) { }

    public record EventDetail(
            UUID id, String slug, String title, String summary, String why,
            String certainty, Integer hijriYear, Integer gregYear, boolean major,
            int approvals, String chronicleSlug, String chronicleTitle,
            List<RelatedPerson> people,
            List<RelatedVerse> verses,
            List<RelatedPlace> places,
            List<RouteLine> routes,
            List<MediaItem> media,
            List<RelatedEventItem> relatedEvents,
            List<SourceItem> sources) { }

    // --- companions ---------------------------------------------------------

    public record PersonListItem(UUID id, String slug, String name, String nameArabic,
                                 String role, Integer deathYearAh) { }

    public record PersonEventItem(String slug, String title, String relation) { }

    public record PersonDetail(UUID id, String slug, String name, String nameArabic,
                               String role, Integer deathYearAh, List<PersonEventItem> events) { }

    // --- search -------------------------------------------------------------

    public record SearchHit(String type, String slug, String title, String subtitle, String arabic) { }
}
