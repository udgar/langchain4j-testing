package com.langchain4j.test.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class EmbeddingConfiguration {

    private final Logger log=LoggerFactory.getLogger(EmbeddingConfiguration.class);

    @Bean
    public EmbeddingModel embeddingModel(@Value("${ollama.base.url}") String baseUrl
            , @Value("${ollama.embedding.model.name}") String embeddingModel) {
        return OllamaEmbeddingModel
                .builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofMinutes(15))
                .maxRetries(0)
                .logRequests(true)
                .build();
    }

    @Bean
    public ChatModel chatModel(@Value("${ollama.base.url}") String baseUrl
            , @Value("${ollama.model.name}") String chatModelName){
       return OllamaChatModel
               .builder()
               .baseUrl(baseUrl)
               .modelName(chatModelName)
               .timeout(Duration.ofMinutes(2))
               .maxRetries(0)
               .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore
                .builder()
                .database("rag_demo")
                .table("vector_store")
                .createTable(true)
                .dimension(768)
                .host("localhost")
                .user("user")
                .password("password")
                .port(5432)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore
            , EmbeddingModel embeddingModel) {
        ContentRetriever delegate=EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();
        return query->{
            List<Content> contents=delegate.retrieve(query);
            log.info("RAG query='{}' -> {} chunk(s)", query.text(), contents.size());
            contents.forEach(c -> log.info("score={} id={} text={}",
                    c.metadata().get(ContentMetadata.SCORE),
                    c.metadata().get(ContentMetadata.EMBEDDING_ID),
                    c.textSegment().text()));
            return contents;
        };
    }
}
