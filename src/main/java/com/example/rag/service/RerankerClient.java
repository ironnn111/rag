package com.example.rag.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class RerankerClient {

    @Value("${spring.ai.openai.embedding.base-url:http://127.0.0.1:18080}")
    private String baseUrl;

    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<String> rerank(String query, List<String> documents, int topK) {
        if (documents.isEmpty()) {
            return List.of();
        }

        Map<String, Object> body = Map.of(
                "query", query,
                "documents", documents,
                "top_k", Math.min(topK, documents.size())
        );

        Map<String, Object> response = restClient.post()
                .uri("/v1/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("results")) {
            return documents.subList(0, Math.min(topK, documents.size()));
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        return results.stream()
                .map(r -> (String) r.get("text"))
                .toList();
    }
}
