package com.seerah.people.application;

import com.seerah.people.application.port.in.CreatePersonUseCase;
import com.seerah.people.application.port.in.PersonLifecycleUseCases;
import com.seerah.people.application.port.out.PersonPorts.LoadPersonPort;
import com.seerah.people.application.port.out.PersonPorts.PersonNamePort;
import com.seerah.people.application.port.out.PersonPorts.PersonProvenanceCheckPort;
import com.seerah.people.application.port.out.PersonPorts.SavePersonPort;
import com.seerah.people.domain.Lifespan;
import com.seerah.people.domain.Person;
import com.seerah.people.domain.PersonId;
import com.seerah.platform.error.NotFoundException;
import com.seerah.shared.ScriptKind;
import com.seerah.shared.Slug;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Write-side application service for people — the transaction boundary (§23.3). */
@Service
@Transactional
public class PersonCommandService implements CreatePersonUseCase, PersonLifecycleUseCases {

    private final SavePersonPort savePerson;
    private final LoadPersonPort loadPerson;
    private final PersonNamePort namePort;
    private final PersonProvenanceCheckPort provenance;

    public PersonCommandService(SavePersonPort savePerson, LoadPersonPort loadPerson,
                                PersonNamePort namePort, PersonProvenanceCheckPort provenance) {
        this.savePerson = savePerson;
        this.loadPerson = loadPerson;
        this.namePort = namePort;
        this.provenance = provenance;
    }

    @Override
    public UUID create(Command c) {
        PersonId id = PersonId.newId();
        Lifespan life = new Lifespan(c.birthYearCe(), c.deathYearCe(), c.birthYearAh(), c.deathYearAh());
        Person person = Person.createDraft(id, new Slug(c.slug()), c.role(), life, c.honorificKey());

        savePerson.save(person);
        namePort.putName(id.value(), c.name(), ScriptKind.LATIN.name(), true);
        if (c.nameArabic() != null && !c.nameArabic().isBlank()) {
            namePort.putName(id.value(), c.nameArabic(), ScriptKind.ARABIC.name(), false);
        }
        return id.value();
    }

    @Override
    public void submit(UUID personId) {
        Person p = loadOrThrow(personId);
        p.submitForReview();
        savePerson.save(p);
    }

    @Override
    public void approve(UUID personId) {
        Person p = loadOrThrow(personId);
        p.approve();
        savePerson.save(p);
    }

    @Override
    public void publish(UUID personId) {
        Person p = loadOrThrow(personId);
        p.publish(provenance.countSupportingCitations(personId), Instant.now());
        savePerson.save(p);
    }

    private Person loadOrThrow(UUID personId) {
        return loadPerson.load(PersonId.of(personId))
                .orElseThrow(() -> new NotFoundException("person.not_found", "No person with id " + personId));
    }
}
