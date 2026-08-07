package com.seerah.people.application.port.out;

import com.seerah.people.domain.Person;
import com.seerah.people.domain.PersonId;

import java.util.Optional;
import java.util.UUID;

/**
 * The outbound ports of the {@code people} module, grouped in one file for
 * readability. Each is implemented by an adapter; the application depends only
 * on these interfaces (§23.1).
 */
public final class PersonPorts {

    private PersonPorts() { }

    public interface SavePersonPort {
        void save(Person person);
    }

    public interface LoadPersonPort {
        Optional<Person> load(PersonId id);
    }

    public interface PersonQueryPort {
        record PersonRow(UUID id, String slug, String role, String status, Integer deathYearAh) { }

        Optional<PersonRow> byId(UUID id);

        Optional<PersonRow> bySlug(String slug);

        java.util.List<PersonRow> publishedOrdered();
    }

    /** Primary display names, stored in {@code person_alias}. */
    public interface PersonNamePort {
        void putName(UUID personId, String name, String script, boolean primary);

        Optional<String> primaryName(UUID personId, String script);
    }

    /** People's view onto provenance, to enforce the publish invariant. */
    public interface PersonProvenanceCheckPort {
        long countSupportingCitations(UUID personId);
    }
}
