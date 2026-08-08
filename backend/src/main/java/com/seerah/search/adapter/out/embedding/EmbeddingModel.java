package com.seerah.search.adapter.out.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A small on-device sentence-embedding model (all-MiniLM-L6-v2, 384-dim) run
 * locally via ONNX Runtime with the HuggingFace tokenizer. Turns any text into a
 * unit-length vector so texts can be compared by meaning — no API, no per-query
 * cost. The model + tokenizer ride along on the classpath ({@code models/minilm}).
 */
@Component
@ConditionalOnProperty(name = "seerah.search.semantic", havingValue = "true", matchIfMissing = true)
public class EmbeddingModel {

    public static final int DIM = 384;

    private HuggingFaceTokenizer tokenizer;
    private OrtEnvironment env;
    private OrtSession session;
    private Set<String> inputNames;

    @PostConstruct
    public void load() throws Exception {
        Path dir = Files.createTempDirectory("minilm");
        Path tokFile = dir.resolve("tokenizer.json");
        try (var in = new ClassPathResource("models/minilm/tokenizer.json").getInputStream()) {
            Files.copy(in, tokFile);
        }
        tokenizer = HuggingFaceTokenizer.newInstance(tokFile);

        byte[] model;
        try (var in = new ClassPathResource("models/minilm/model.onnx").getInputStream()) {
            model = in.readAllBytes();
        }
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        // Single-threaded inference so embeddings are bit-for-bit deterministic:
        // parallel intra-op reduction reorders float sums, and for a corpus of very
        // similar short texts that jitter is enough to reshuffle the ranking.
        opts.setIntraOpNumThreads(1);
        opts.setInterOpNumThreads(1);
        session = env.createSession(model, opts);
        inputNames = session.getInputNames();
    }

    /** Embed one text into a normalized 384-dim vector (mean-pooled token embeddings). */
    public float[] embed(String text) {
        try {
            Encoding enc = tokenizer.encode(text == null ? "" : text);
            long[] ids = enc.getIds();
            long[] mask = enc.getAttentionMask();
            long[] types = enc.getTypeIds();
            long[] shape = {1, ids.length};

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape));
            if (inputNames.contains("attention_mask")) {
                inputs.put("attention_mask", OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape));
            }
            if (inputNames.contains("token_type_ids")) {
                inputs.put("token_type_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(types), shape));
            }
            try (OrtSession.Result res = session.run(inputs)) {
                float[][][] out = (float[][][]) res.get(0).getValue(); // [1, tokens, DIM]
                float[] pooled = meanPool(out[0], mask);
                normalize(pooled);
                return pooled;
            } finally {
                inputs.values().forEach(OnnxTensor::close);
            }
        } catch (Exception e) {
            throw new IllegalStateException("embedding failed for text: " + text, e);
        }
    }

    /** Cosine similarity of two normalized vectors is just their dot product. */
    public static float cosine(float[] a, float[] b) {
        float s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    private static float[] meanPool(float[][] tokens, long[] mask) {
        int dim = tokens.length == 0 ? DIM : tokens[0].length;
        float[] sum = new float[dim];
        float count = 0;
        for (int t = 0; t < tokens.length; t++) {
            if (mask[t] == 0) continue;
            count++;
            for (int d = 0; d < dim; d++) sum[d] += tokens[t][d];
        }
        if (count > 0) for (int d = 0; d < dim; d++) sum[d] /= count;
        return sum;
    }

    private static void normalize(float[] v) {
        double n = 0;
        for (float x : v) n += x * x;
        n = Math.sqrt(n);
        if (n > 0) for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / n);
    }

    @PreDestroy
    void close() {
        try { if (session != null) session.close(); } catch (Exception ignored) { }
        try { if (tokenizer != null) tokenizer.close(); } catch (Exception ignored) { }
    }
}
