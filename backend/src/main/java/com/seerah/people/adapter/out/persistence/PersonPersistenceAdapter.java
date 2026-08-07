package com.seerah.people.adapter.out.persistence;

import com.seerah.people.application.port.out.PersonPorts.LoadPersonPort;
import com.seerah.people.application.port.out.PersonPorts.PersonQueryPort;
import com.seerah.people.application.port.out.PersonPorts.SavePersonPort;
import com.seerah.people.domain.Lifespan;
import com.seerah.people.domain.Person;
import com.seerah.people.domain.PersonId;
import com.seerah.shared.ContentStatus;
import com.seerah.shared.Slug;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence adapter for the person aggregate: save/load (write) and query (read). */
@Component
public class PersonPersistenceAdapter implements SavePersonPort, LoadPersonPort, PersonQueryPort {

    private final PersonJpaRepository repository;

    public PersonPersistenceAdapter(PersonJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Person person) {
        PersonJpaEntity e = repository.findById(person.id().value()).orElseGet(PersonJpaEntity::new);
        Lifespan l = person.lifespan();
        e.setId(person.id().value());
        e.setSlug(person.slug().value());
        e.setRoleType(person.role());
        e.setBirthYearCe(l.birthYearCe());
        e.setDeathYearCe(l.deathYearCe());
        e.setBirthYearAh(l.birthYearAh());
        e.setDeathYearAh(l.deathYearAh());
        e.setHonorificKey(person.honorificKey());
        e.setStatus(person.status());
        e.setPublishedAt(person.publishedAt());
        e.setUpdatedAt(Instant.now());
        repository.save(e);
    }

    @Override
    public Optional<Person> load(PersonId id) {
        return repository.findById(id.value()).map(PersonPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<PersonRow> byId(UUID id) {
        return repository.findById(id).map(PersonPersistenceAdapter::toRow);
    }

    @Override
    public Optional<PersonRow> bySlug(String slug) {
        return repository.findBySlug(slug).map(PersonPersistenceAdapter::toRow);
    }

    @Override
    public List<PersonRow> publishedOrdered() {
        return repository.findByStatusOrderBySlugAsc(ContentStatus.PUBLISHED)
                .stream().map(PersonPersistenceAdapter::toRow).toList();
    }

    private static Person toDomain(PersonJpaEntity e) {
        Lifespan l = new Lifespan(e.getBirthYearCe(), e.getDeathYearCe(), e.getBirthYearAh(), e.getDeathYearAh());
        return Person.rehydrate(PersonId.of(e.getId()), new Slug(e.getSlug()), e.getRoleType(), l,
                e.getHonorificKey(), e.getStatus(), e.getPublishedAt(), e.getVersion());
    }

    private static PersonRow toRow(PersonJpaEntity e) {
        return new PersonRow(e.getId(), e.getSlug(), e.getRoleType().name(),
                e.getStatus().name(), e.getDeathYearAh());
    }
}
