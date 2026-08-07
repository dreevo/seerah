package com.seerah.people.adapter.out.provenance;

import com.seerah.people.application.port.out.PersonPorts.PersonProvenanceCheckPort;
import com.seerah.provenance.api.CitationDirectory;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Bridges people onto provenance, only through {@code provenance.api} (§6.8.2). */
@Component
public class PersonProvenanceCheckAdapter implements PersonProvenanceCheckPort {

    private final CitationDirectory citationDirectory;

    public PersonProvenanceCheckAdapter(CitationDirectory citationDirectory) {
        this.citationDirectory = citationDirectory;
    }

    @Override
    public long countSupportingCitations(UUID personId) {
        return citationDirectory.countSupportingCitations(EntityType.PERSON, personId);
    }
}
