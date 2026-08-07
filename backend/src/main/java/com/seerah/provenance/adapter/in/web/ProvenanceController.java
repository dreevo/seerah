package com.seerah.provenance.adapter.in.web;

import com.seerah.provenance.api.CitationRegistrar;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound web adapter for provenance. Editors register works and attach citations
 * here; the same {@link CitationRegistrar} contract is what other modules use
 * internally, so there is exactly one way to record a citation.
 */
@RestController
@RequestMapping("/api")
public class ProvenanceController {

    private final CitationRegistrar registrar;

    public ProvenanceController(CitationRegistrar registrar) {
        this.registrar = registrar;
    }

    @PostMapping("/sources")
    public ResponseEntity<Map<String, UUID>> registerSource(
            @RequestBody CitationRegistrar.RegisterSource body) {
        UUID id = registrar.registerSource(body);
        return ResponseEntity.created(URI.create("/api/sources/" + id)).body(Map.of("id", id));
    }

    @PostMapping("/citations")
    public ResponseEntity<Map<String, UUID>> addCitation(
            @RequestBody CitationRegistrar.AddCitation body) {
        UUID id = registrar.addCitation(body);
        return ResponseEntity.created(URI.create("/api/citations/" + id)).body(Map.of("id", id));
    }

    @PostMapping("/scholarly-positions")
    public ResponseEntity<Map<String, UUID>> addPosition(
            @RequestBody CitationRegistrar.AddScholarlyPosition body) {
        UUID id = registrar.addScholarlyPosition(body);
        return ResponseEntity.created(URI.create("/api/scholarly-positions/" + id)).body(Map.of("id", id));
    }
}
