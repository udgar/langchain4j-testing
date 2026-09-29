package com.langchain4j.test.rag.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
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
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                // .queryTransformer(new ExpandingQueryTransformer(chatModel))  // enable only if recall is poor
                .build();
        QueryAssistant queryAssistant = AiServices.builder(QueryAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .build();
        return queryAssistant.query(questions);
    }

    // Here we need to add an Assistant functional interface in order to use it properly
    interface Assistant {
        @SystemMessage("You are a chat assistant that answers user questions")
        String chat(String message);
    }

    interface QueryAssistant {
        @SystemMessage("""
                You answer only from the provided excerpts of the Constitution of Nepal.
                Cite the Article number for every claim. If the excerpts don't contain the answer, say so.
                Refuse anything unrelated to the Constitution.
                """)
        String query(String message);
    }
}
