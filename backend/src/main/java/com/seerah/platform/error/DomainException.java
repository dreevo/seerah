package com.seerah.platform.error;

/**
 * Base of the domain exception hierarchy (§27.1). Domain code throws these; the
 * web layer translates them to RFC 9457 problem details. Every instance carries
 * a stable machine-readable {@link #code()} that forms part of the API contract
 * (§27.3) and must never change once shipped.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stable error code, e.g. {@code event.publish.requires_citation}. */
    public String code() {
        return code;
    }
}
