package com.seerah.ingestion.batch;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Records the audit trail every batch import must leave (§12.10): an
 * {@code ingestion_run} per source, and a {@code skip_audit} row — with the raw
 * payload and the reason — for anything the importer refused. An import that
 * silently drops data is worse than one that fails.
 */
@Component
public class IngestionLog {

    private final JdbcTemplate jdbc;

    public IngestionLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Whether a table already holds rows — the idempotency guard for re-runs. */
    public boolean hasRows(String table) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
        return n != null && n > 0;
    }

    public UUID startRun(String sourceName, byte[] checksum) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ingestion_run (id, source_name, source_checksum) VALUES (?, ?, ?)",
                id, sourceName, checksum);
        return id;
    }

    public void skip(UUID runId, int rowNumber, String reason, String payloadJson) {
        jdbc.update("INSERT INTO skip_audit (run_id, row_number, reason, payload) VALUES (?, ?, ?, CAST(? AS jsonb))",
                runId, rowNumber, reason, payloadJson);
    }

    public void finish(UUID runId, int read, int ok, int skipped, String outcome) {
        jdbc.update("""
                UPDATE ingestion_run SET finished_at = now(), rows_read = ?, rows_ok = ?,
                       rows_skipped = ?, outcome = ? WHERE id = ?
                """, read, ok, skipped, outcome, runId);
    }
}
