package com.seerah.shared;

/**
 * How firmly a claim is attested (§12.2 {@code certainty}). Order runs from
 * strongest to most contested. {@link #SCHOLARS_DIFFER} carries a hard
 * invariant: a claim marked so must expose two or more scholarly positions
 * (§13.4).
 */
public enum Certainty {
    MUTAWATIR,
    WELL_ATTESTED,
    REPORTED,
    WEAK,
    SCHOLARS_DIFFER,
    DISPUTED;

    /** Whether publishing requires recorded scholarly positions (§13.4). */
    public boolean requiresPositions() {
        return this == SCHOLARS_DIFFER;
    }
}
