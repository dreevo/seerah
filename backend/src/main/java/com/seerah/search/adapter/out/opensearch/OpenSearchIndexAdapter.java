package com.seerah.search.adapter.out.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seerah.search.application.port.out.SearchIndex;
import com.seerah.search.application.port.out.SearchIndexWriter;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The OpenSearch search engine (§17), behind the same {@link SearchIndex} read port
 * the Postgres engine implements — so search can be flipped between them by a flag,
 * with no change above this layer (§20 canary + fallback). It also implements
 * {@link SearchIndexWriter}, the seam the projection relay writes through.
 *
 * <p>Uses the low-level REST client with hand-built JSON: predictable, and free of a
 * heavy typed-client dependency. A light English analyzer is defined on the text
 * field; the fuller Arabic analysis of §18 is a mapping change here alone.
 */
@Component
@ConditionalOnProperty(name = "search.engine", havingValue = "opensearch")
public class OpenSearchIndexAdapter implements SearchIndex, SearchIndexWriter {

    private static final String INDEX = "seerah-search";

    private final RestClient client;
    private final ObjectMapper json;

    public OpenSearchIndexAdapter(RestClient client, ObjectMapper json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public void ensureIndex() {
        // Always attempt to create with the explicit mapping; ignore "already exists".
        // (We do not probe with HEAD because a keyword `type` field is essential —
        // an auto-created dynamic mapping would make it analysed text and break the
        // type filter.)
        Request create = new Request("PUT", "/" + INDEX);
        create.setJsonEntity("""
            {
              "settings": { "index": { "number_of_shards": 1, "number_of_replicas": 0 } },
              "mappings": { "properties": {
                "type": { "type": "keyword" },
                "slug": { "type": "keyword" },
                "text": { "type": "text", "analyzer": "english" }
              } }
            }
            """);
        try {
            client.performRequest(create);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != 400) { // 400 = already exists
                throw new UncheckedIOException(new IOException(e));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void index(String type, UUID id, String slug, String text) {
        Request req = new Request("PUT", "/" + INDEX + "/_doc/" + id);
        req.setJsonEntity(json.createObjectNode()
                .put("type", type).put("slug", slug).put("text", text == null ? "" : text)
                .toString());
        perform(req);
    }

    @Override
    public void refresh() {
        perform(new Request("POST", "/" + INDEX + "/_refresh"));
    }

    @Override
    public List<Row> matchingEvents(String term, int limit) {
        return query("EVENT", term, limit);
    }

    @Override
    public List<Row> matchingPeople(String term, int limit) {
        return query("PERSON", term, limit);
    }

    private List<Row> query(String type, String term, int limit) {
        Request req = new Request("POST", "/" + INDEX + "/_search");
        req.setJsonEntity("""
            { "size": %d,
              "query": { "bool": {
                "must":   [ { "match": { "text": %s } } ],
                "filter": [ { "term": { "type": %s } } ] } } }
            """.formatted(limit, quote(term), quote(type)));
        JsonNode body = performJson(req);
        List<Row> rows = new ArrayList<>();
        for (JsonNode hit : body.path("hits").path("hits")) {
            rows.add(new Row(UUID.fromString(hit.path("_id").asText()),
                    hit.path("_source").path("slug").asText()));
        }
        return rows;
    }

    private String quote(String s) {
        return json.valueToTree(s).toString(); // JSON-escaped string literal
    }

    private Response perform(Request req) {
        try {
            return client.performRequest(req);
        } catch (IOException e) {
            throw new UncheckedIOException("OpenSearch request failed: " + req.getMethod() + " " + req.getEndpoint(), e);
        }
    }

    private JsonNode performJson(Request req) {
        try {
            return json.readTree(perform(req).getEntity().getContent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
