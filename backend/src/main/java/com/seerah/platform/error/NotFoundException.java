package com.seerah.platform.error;

/** A referenced aggregate does not exist. Maps to HTTP 404. */
public class NotFoundException extends DomainException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
