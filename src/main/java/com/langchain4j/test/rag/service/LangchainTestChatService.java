package com.langchain4j.test.rag.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LangchainTestChatService {

    private final String ollamaBaseUrl;

    private final String ollamaCurrentModel;

    private final ContentRetriever contentRetriever;

    public LangchainTestChatService(@Value("${ollama.base.url}") String ollamaBaseUrl, @Value("${ollama.model.name}") String ollamaCurrentModel, ContentRetriever contentRetriever) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaCurrentModel = ollamaCurrentModel;
        this.contentRetriever = contentRetriever;
    }

    public String chatUsingLangChain(String message) {
        var ollamaChatModel = OllamaChatModel
                .builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaCurrentModel)
                .build();
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(ollamaChatModel).build();
        return assistant.chat(message);
    }

    public String inquiry(String questions) {
        var queryChatModel = OllamaChatModel
                .builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaCurrentModel)
                .build();
        QueryAssistant queryAssistant = AiServices.builder(QueryAssistant.class)
                .chatModel(queryChatModel)
                .contentRetriever(contentRetriever)
                .build();
        return queryAssistant.query(questions);
    }

    // Here we need to add an Assistant functional interface in order to use it properly
    interface Assistant {
        @SystemMessage("You are a chat assistant that answers user questions")
        String chat(String message);
    }

    interface QueryAssistant {
        @SystemMessage("You are a chat assistant that only answers questions, regarding the information stored in vector store, if asked for anything else deny the request")
        String query(String message);
    }
}
