package com.seerah.platform.error;

/**
 * A domain invariant was violated — the request was well-formed but would break
 * a rule the system exists to protect (e.g. publishing without a citation,
 * §13.2). Maps to HTTP 422.
 */
public class DomainRuleViolation extends DomainException {
    public DomainRuleViolation(String code, String message) {
        super(code, message);
    }
}
