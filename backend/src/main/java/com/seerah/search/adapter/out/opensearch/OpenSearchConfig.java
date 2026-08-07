package com.seerah.search.adapter.out.opensearch;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the OpenSearch engine — and only when {@code search.engine=opensearch}.
 * With the default (Postgres) engine none of this loads, so the Phase-1 build
 * needs neither an OpenSearch server nor the scheduled projection relay.
 * Scheduling is enabled here so the relay ticks only in the search phase.
 */
@Configuration
@ConditionalOnProperty(name = "search.engine", havingValue = "opensearch")
@EnableScheduling
public class OpenSearchConfig {

    @Bean(destroyMethod = "close")
    RestClient openSearchRestClient(@Value("${search.opensearch.uri:http://localhost:9200}") String uri) {
        return RestClient.builder(HttpHost.create(uri)).build();
    }
}
