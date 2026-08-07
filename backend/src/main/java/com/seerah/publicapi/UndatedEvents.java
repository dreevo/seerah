package com.seerah.publicapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * The set of events whose "when" is not fixed by the Qur'an or an authentic
 * ḥadīth — namings, gifts, status verses, and episodes the sources relate
 * without placing them in time. The reader's timeline draws these on a separate
 * "?" branch rather than on the dated spine, so the ordering never asserts a
 * chronology the sources do not give. Bundled at {@code seed/undated-events.json}
 * (a single editable list); the BFF flags each timeline row against it.
 */
@Component
public class UndatedEvents {

    private Set<String> slugs = Set.of();

    @PostConstruct
    void load() throws Exception {
        try (var in = new ClassPathResource("seed/undated-events.json").getInputStream()) {
            slugs = Set.copyOf(new ObjectMapper().readValue(in, new TypeReference<List<String>>() { }));
        }
    }

    public boolean isUndated(String slug) {
        return slug != null && slugs.contains(slug);
    }
}
