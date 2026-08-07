package com.seerah;

import com.seerah.search.adapter.out.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the local embedding model is genuinely semantic — no Spring, no DB, no
 * network. A query should sit closer (higher cosine) to the passage that matches
 * its meaning than to an unrelated one, even without shared keywords.
 */
class EmbeddingModelTest {

    static EmbeddingModel model;

    @BeforeAll
    static void setUp() throws Exception {
        model = new EmbeddingModel();
        model.load();
    }

    @Test
    void embedsToUnitVectorsOfExpectedDimension() {
        float[] v = model.embed("the parting of the sea");
        assertThat(v).hasSize(EmbeddingModel.DIM);
        double norm = 0; for (float x : v) norm += x * x;
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-3));
    }

    @Test
    void ranksByMeaningNotKeywords() {
        // The query shares NO content words with the right answer ("sea", "part").
        float[] q = model.embed("who divided the water so his people could walk across?");
        float[] musa = model.embed("Musa struck the sea with his staff and it split into two walls with a dry path between them");
        float[] adam = model.embed("Allah created Adam from clay and taught him the names of all things");

        float simMusa = EmbeddingModel.cosine(q, musa);
        float simAdam = EmbeddingModel.cosine(q, adam);
        assertThat(simMusa).isGreaterThan(simAdam);
    }
}
