package com.seerah.content.adapter.in.web.dto;

import com.seerah.shared.Certainty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Inbound request to create an event draft. Validation lives on the DTO, not the domain. */
public record CreateEventRequest(
        @NotBlank String slug,
        @NotBlank String title,
        String locale,
        int hijriYear,
        Integer gregorianYear,
        @NotNull Certainty certainty,
        boolean major,
        int sortKey) {

    public String localeOrDefault() {
        return (locale == null || locale.isBlank()) ? "en" : locale;
    }
}
