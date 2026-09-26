package com.langchain4j.test.rag.controller;

import com.langchain4j.test.rag.service.IngestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionService service;

    public IngestionController(IngestionService service) {
        this.service = service;
    }

    @GetMapping
    public String ingestion() {
        service.ingestDocument();
        return "Done Ingestion";
    }
}
