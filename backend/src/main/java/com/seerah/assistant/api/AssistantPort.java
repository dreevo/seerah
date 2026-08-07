package com.seerah.assistant.api;

import java.util.List;

/**
 * The grounded question-answering contract. The assistant "retrieves and
 * rephrases; it never asserts" (§5.7). It has <em>no knowledge of its own</em>: it
 * answers only from the published, cited corpus, and when the corpus does not
 * cover a question it returns the fixed refusal (rule 2) — it never supplements
 * from anywhere else. It has no persistence and writes nothing.
 */
public interface AssistantPort {

    /** The exact wording the platform uses when it cannot answer (rule 2). */
    String REFUSAL = "The published material on this platform does not cover that.";

    Answer ask(String question);

    /**
     * @param answered  false when nothing in the corpus covers the question
     * @param message   the refusal text when not answered; empty otherwise
     * @param passages  cited passages, each drawn verbatim from a published summary
     * @param sources   the numbered source list the passages' markers refer to
     */
    record Answer(boolean answered, String message, List<Passage> passages, List<Cite> sources) { }

    /** One sourced passage. {@code markers} are the [S#] indices supporting it (rule 1). */
    record Passage(String eventSlug, String eventTitle, String text, String confidence, List<Integer> markers) { }

    /** A numbered source, referenced as [S{index}]. */
    record Cite(int index, String workTitle, String tier, String locator) { }
}
