package com.seerah.shared;

/** The role a citation plays with respect to a claim (§12.2 {@code citation_role}). */
public enum CitationRole {
    SUPPORTS, DETAILS, DISPUTES, CONTEXTUALISES, PRIMARY_FOR;

    /** Roles that count as evidence *for* a claim when checking the citation-required rule (§13.2). */
    public boolean isSupporting() {
        return this == SUPPORTS || this == PRIMARY_FOR || this == DETAILS;
    }
}
