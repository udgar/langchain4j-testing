package com.langchain4j.test.rag.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Component;

@Component
public class LangchainTestChatService {

    private final ContentRetriever contentRetriever;

    private final ChatModel chatModel;

    public LangchainTestChatService(ContentRetriever contentRetriever, ChatModel chatModel) {
        this.contentRetriever = contentRetriever;
        this.chatModel = chatModel;
    }

    public String chatUsingLangChain(String message) {
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(chatModel).build();
        return assistant.chat(message);
    }

    public String inquiry(String questions) {
        QueryAssistant queryAssistant = AiServices.builder(QueryAssistant.class)
                .chatModel(chatModel)
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
