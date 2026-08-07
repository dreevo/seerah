package com.seerah.platform.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Translates the domain exception hierarchy into RFC 9457 problem details
 * (§27.2). The {@code type} URI and the {@code code} extension are the stable,
 * documented contract; the {@code detail} message is human-facing and may change.
 *
 * <p>What must never appear in a body: stack traces, SQL, internal identifiers
 * of other users, or source quote text from non-quotable works (§27.6).
 */
@RestControllerAdvice
public class ProblemDetailAdvice {

    private static final String TYPE_BASE = "https://seerah.platform/problems/";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail onNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex);
    }

    @ExceptionHandler(DomainRuleViolation.class)
    public ProblemDetail onRuleViolation(DomainRuleViolation ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Domain rule violated", ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onBadInput(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid request");
        pd.setType(URI.create(TYPE_BASE + "invalid-request"));
        pd.setProperty("code", "request.invalid");
        return pd;
    }

    private ProblemDetail problem(HttpStatus status, String title, DomainException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(title);
        pd.setType(URI.create(TYPE_BASE + ex.code().replace('.', '/')));
        pd.setProperty("code", ex.code());
        return pd;
    }
}
