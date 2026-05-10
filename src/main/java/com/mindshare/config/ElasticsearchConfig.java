package com.mindshare.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ElasticsearchConfig.MindShareElasticsearchProperties.class)
public class ElasticsearchConfig {

    @Bean
    @ConditionalOnProperty(prefix = "mindshare.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RestClient restClient(ElasticsearchProperties elasticsearchProperties) {
        HttpHost[] hosts = elasticsearchProperties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);
        return builder.build();
    }

    @Bean
    @ConditionalOnBean(RestClient.class)
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    @Data
    @ConfigurationProperties(prefix = "mindshare.elasticsearch")
    public static class MindShareElasticsearchProperties {

        private boolean enabled = true;
        private String indexName = "mindshare_knowpost";
    }
}
