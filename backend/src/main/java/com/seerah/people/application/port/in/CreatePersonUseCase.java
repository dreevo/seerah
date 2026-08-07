package com.seerah.people.application.port.in;

import com.seerah.people.domain.PersonRole;

import java.util.UUID;

/** Create a person in DRAFT, with a primary name (Latin) and Arabic name. */
public interface CreatePersonUseCase {

    UUID create(Command command);

    record Command(
            String slug,
            String name,
            String nameArabic,
            PersonRole role,
            Integer birthYearCe,
            Integer deathYearCe,
            Integer birthYearAh,
            Integer deathYearAh,
            String honorificKey) {
    }
}
