package com.seerah;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seerah.ingestion.validation.SeedValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Governance as an authoring-time gate — no database, no Spring. Every chronicle
 * seed file must pass the editorial invariants ({@link SeedValidator}) before it
 * can ship. This is the check that replaced the old runtime review pipeline: for
 * a fixed corpus the content is verified once, here, at build time.
 */
class SeedValidationTest {

    private static final Path SEED_DIR = Path.of("src/main/resources/seed");
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void everySeedFileSatisfiesTheEditorialInvariants() throws IOException {
        List<String> allProblems = new ArrayList<>();
        int files = 0;

        try (Stream<Path> paths = Files.list(SEED_DIR)) {
            List<Path> seedFiles = paths
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".json") && (n.startsWith("chronicle-") || n.equals("chronology.json"));
                    })
                    .sorted()
                    .toList();

            for (Path p : seedFiles) {
                files++;
                var root = json.readTree(Files.readAllBytes(p));
                allProblems.addAll(SeedValidator.validate(p.getFileName().toString(), root));
            }
        }

        assertThat(files).as("number of chronicle seed files scanned").isGreaterThanOrEqualTo(21);
        assertThat(allProblems).as("editorial-invariant violations across all seed files").isEmpty();
    }
}
