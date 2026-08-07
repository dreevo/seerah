package com.seerah.shared;

import java.util.regex.Pattern;

/**
 * A URL-safe, stable public identifier for a piece of content. Part of the
 * shared kernel because every content entity — event, person, place — is
 * addressed by one, and the addressing scheme must be identical across contexts
 * (§6.8.1). Slugs are permanent; when one changes the old value becomes a
 * redirect alias rather than breaking (§12.9 {@code slug_alias}).
 */
public record Slug(String value) {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public Slug {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Slug must be lowercase alphanumeric words separated by single hyphens: " + value);
        }
        if (value.length() > 120) {
            throw new IllegalArgumentException("Slug must be at most 120 characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
