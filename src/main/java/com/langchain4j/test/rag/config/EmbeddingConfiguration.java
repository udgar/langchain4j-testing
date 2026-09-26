package com.langchain4j.test.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {

    @Bean
    public EmbeddingModel embeddingModel(@Value("${ollama.base.url}") String baseUrl
            , @Value("${ollama.embedding.model.name}") String embeddingModel) {
        return OllamaEmbeddingModel
                .builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore
                .builder()
                .database("rag_demo")
                .table("vector_store")
                .createTable(false)
                .dimension(768)
                .host("jdbc:postgresql://localhost:5432/rag_demo")
                .user("user")
                .password("password")
                .port(5432)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore
            , EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.9)
                .build();
    }
}
