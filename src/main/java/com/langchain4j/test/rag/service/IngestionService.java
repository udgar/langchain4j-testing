package com.langchain4j.test.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class IngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public IngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public void ingestDocument() {
        DocumentParser parser = new ApachePdfBoxDocumentParser();
        Document document = FileSystemDocumentLoader
                .loadDocument(IngestionService.class.getResource("/docs/Constitution-of-Nepal.pdf")
                        .getPath(), parser);
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);
        var embedding = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embedding);
    }
}
