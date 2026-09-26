package com.langchain4j.test.rag.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LangchainTestChatService {

    private final String ollamaBaseUrl;

    private final String ollamaCurrentModel;

    public LangchainTestChatService(@Value("${ollama.base.url}") String ollamaBaseUrl, @Value("${ollama.model.name}") String ollamaCurrentModel) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaCurrentModel = ollamaCurrentModel;
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


    // Here we need to add an Assistant functional interface in order to use it properly
    interface Assistant {
        @SystemMessage("You are a chat assistant that answers user questions")
        String chat(String message);
    }
}
