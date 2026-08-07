package com.seerah.people.adapter.in.web;

import com.seerah.people.api.PersonReadPort;
import com.seerah.people.api.PersonSummaryView;
import com.seerah.people.application.port.in.CreatePersonUseCase;
import com.seerah.people.application.port.in.PersonLifecycleUseCases;
import com.seerah.people.domain.PersonRole;
import com.seerah.platform.error.NotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final CreatePersonUseCase createPerson;
    private final PersonLifecycleUseCases lifecycle;
    private final PersonReadPort personRead;

    public PersonController(CreatePersonUseCase createPerson, PersonLifecycleUseCases lifecycle,
                            PersonReadPort personRead) {
        this.createPerson = createPerson;
        this.lifecycle = lifecycle;
        this.personRead = personRead;
    }

    public record CreatePersonRequest(
            @NotBlank String slug, @NotBlank String name, String nameArabic,
            @NotNull PersonRole role, Integer birthYearCe, Integer deathYearCe,
            Integer birthYearAh, Integer deathYearAh, String honorificKey) {
    }

    @PostMapping
    public ResponseEntity<PersonSummaryView> create(@RequestBody CreatePersonRequest r) {
        UUID id = createPerson.create(new CreatePersonUseCase.Command(
                r.slug(), r.name(), r.nameArabic(), r.role(),
                r.birthYearCe(), r.deathYearCe(), r.birthYearAh(), r.deathYearAh(), r.honorificKey()));
        PersonSummaryView view = personRead.findById(id, "en")
                .orElseThrow(() -> new NotFoundException("person.not_found", "Person vanished after creation"));
        return ResponseEntity.created(URI.create("/api/people/" + r.slug())).body(view);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submit(@PathVariable UUID id) { lifecycle.submit(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id) { lifecycle.approve(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) { lifecycle.publish(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{slug}")
    public PersonSummaryView bySlug(@PathVariable String slug, @RequestParam(defaultValue = "en") String locale) {
        return personRead.findBySlug(slug, locale)
                .orElseThrow(() -> new NotFoundException("person.not_found", "No person with slug " + slug));
    }
}
