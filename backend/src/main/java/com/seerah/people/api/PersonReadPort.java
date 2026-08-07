package com.seerah.people.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The {@code people} module's published read contract (§22.2). */
public interface PersonReadPort {

    Optional<PersonSummaryView> findById(UUID id, String locale);

    Optional<PersonSummaryView> findBySlug(String slug, String locale);

    /** All published people, for the companions index. */
    List<PersonSummaryView> publishedList(String locale);
}
