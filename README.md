# langchain4j-testing
This project is aimed at learning langchain4j libraries and their capabilities. Current Model Used is llama3.2 and nomic-embed-text for embedding
The implementation is same as the one done in this project https://github.com/udgar/rag-spring-boot-setup
But just using Langchain4j libraries.

## What was done here ?
Same thing as the mentioned repository above, however in this we used langchain4j in order to compare the difference between it and spring AI.
In this project we implemented the step by step RAG implementation and analyzed the difference in result.

## Tech Stack Used:
1. JAVA programming language
2. Spring Boot Framework
3. Langchain4j
4. Langchain4j ollama(Connect to chat and embedding model)
5. Langchain4j pgvector(store embeddings)
6. Swagger Documentation

## Resource Used:
Constitution-of-Nepal.pdf

## Process:
The process is same as before, in this one first the initialization of Langchain4j dependencies' bean were done.
For ChatModel, EmbeddingModel,VectorStore and ContextRetriever the bean were initialized.

```java
    @Bean
    public EmbeddingModel embeddingModel(@Value("${ollama.base.url}") String baseUrl
            , @Value("${ollama.embedding.model.name}") String embeddingModel) {
        return OllamaEmbeddingModel
                .builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofMinutes(15))
                .maxRetries(0)
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
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();
    }
```
***NOTES***
1. Here, for the embedding model and chatmodel, we have kept the timeout duration conventionally higher, this is because connecting and getting response from remotely hosted LLM takes time.
2. In VectorStore where PgStore is used, we have automated the creation of table as well, just for development purposes.
3. Max Retries is 0, because for quick development we need to find out root cause of the problem quickly, and retries hinders that.
4. In the content Retriever min score is kept 0.5 which is the proper convention for RAG, if made higher, the content retriever will look for similarity score of higher value and since negligible amount of entry can match it, the result will be minimal if not at all.
5. And Max Result 5, so that only top 5 matched result will be added to prompt and result time for LLM will be faster. Also since the prompt size will be less, less token will be used.

Now For the Ingestion Process we used ingestion service.

```java
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
        int batchSize = 20;
        for (int i = 0; i < segments.size(); i += batchSize) {
            List<TextSegment> batch = segments.subList(i, Math.min(i + batchSize, segments.size()));
            var embedding = embeddingModel.embedAll(batch).content();
            embeddingStore.addAll(embedding, batch);
        }
    }
}
```
***NOTES***
1. Here the chunk size is kept to be 300, which is fine for RAG, however no overlap is kept, this needs to be analyzed.
2. Here we can notice that batch of 20 is processed at a time. This is due to the fact that while chunking the PDF in size of 300, more than 2000 entries are created and embedding and saving them all at once will result in timeout for embedding server.

And finally chat service
```java
    private final ContentRetriever contentRetriever;

    private final ChatModel chatModel;

public LangchainTestChatService(ContentRetriever contentRetriever, ChatModel chatModel) {
    this.contentRetriever = contentRetriever;
    this.chatModel = chatModel;
}

public String inquiry(String questions) {
    QueryAssistant queryAssistant = AiServices.builder(QueryAssistant.class)
            .chatModel(chatModel)
            .contentRetriever(contentRetriever)
            .build();
    return queryAssistant.query(questions);
}

interface QueryAssistant {
    @SystemMessage("You are a chat assistant that only answers questions, regarding the information stored in vector store, if asked for anything else deny the request")
    String query(String message);
}
```

***NOTES***
1. Langchain4j requires an functional interface, upon which AiServices library defines the implementation, in order to create connect with the LLM.
2. @SystemMessage is used so that it is easier to provide system prompt.

### Comparison with Spring AI:
What I found using both spring ai and langchain4j.
1. The quality of result was good. While spring ai provides autoconfiguration and everything is done under the hood, langchain4j is a breath of fresh air where we can see what is happening. Spring Ai provides a lot of abstraction that honestly for me clouds what is going on.
2. The results are faster. For Spring Ai, for retrieving the content I used the QuestionAnswerAdvisor and for this I used ContentRetriever. The Langchain4j was much faster.
3. According to my analysis, the reason for Langchain4j being faster was because, in this we were in control of the chunking as well as content retrieval. In spring ai, I had no idea what size my chunk were. Also I had no idea the min score of my query result.

### Verdict:

Although I think the same result could easily be obtained for spring ai that we obtained from lanchain4j, and the poor quality of result in spring ai was due to heavy reliance on spring ai autoconfiguration.
For now I would prefer to use langchain4j. However for spring the best tool is said to be spring ai itself.
