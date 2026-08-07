package com.seerah.people.application;

import com.seerah.people.api.PersonReadPort;
import com.seerah.people.api.PersonSummaryView;
import com.seerah.people.application.port.out.PersonPorts.PersonNamePort;
import com.seerah.people.application.port.out.PersonPorts.PersonQueryPort;
import com.seerah.shared.ScriptKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read side for people (§25.3). Resolves the primary and Arabic names into a view. */
@Service
@Transactional(readOnly = true)
public class PersonQueryService implements PersonReadPort {

    private final PersonQueryPort queryPort;
    private final PersonNamePort namePort;

    public PersonQueryService(PersonQueryPort queryPort, PersonNamePort namePort) {
        this.queryPort = queryPort;
        this.namePort = namePort;
    }

    @Override
    public Optional<PersonSummaryView> findById(UUID id, String locale) {
        return queryPort.byId(id).map(this::toView);
    }

    @Override
    public Optional<PersonSummaryView> findBySlug(String slug, String locale) {
        return queryPort.bySlug(slug).map(this::toView);
    }

    @Override
    public List<PersonSummaryView> publishedList(String locale) {
        return queryPort.publishedOrdered().stream().map(this::toView).toList();
    }

    private PersonSummaryView toView(PersonQueryPort.PersonRow row) {
        String name = namePort.primaryName(row.id(), ScriptKind.LATIN.name()).orElse(row.slug());
        String ar = namePort.primaryName(row.id(), ScriptKind.ARABIC.name()).orElse(null);
        return new PersonSummaryView(row.id(), row.slug(), name, ar, row.role(), row.status(), row.deathYearAh());
    }
}
