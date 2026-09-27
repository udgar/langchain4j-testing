package com.langchain4j.test.rag.controller;

import com.langchain4j.test.rag.service.LangchainTestChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/langchat")
public class LangchainTestChatController {

    private final LangchainTestChatService service;

    public LangchainTestChatController(LangchainTestChatService service) {
        this.service = service;
    }

    @GetMapping
    public String chat() {
        return service.chatUsingLangChain("Please introduce yourself");
    }

    @PostMapping(value = "/query")
    public String query(@RequestBody String query) {
        return service.inquiry(query);
    }
}
