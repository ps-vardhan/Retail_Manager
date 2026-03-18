package com.retail.retailmanager.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.retail.retailmanager.model.Product;
import com.retail.retailmanager.repository.ProductRepository;

@Service
public class AIService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ProductRepository productRepository;

    public AIService(VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            ProductRepository productRepository) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.productRepository = productRepository;
    }

    /**
     * Runs a cosine similarity search against the Spring AI pgvector store,
     * then resolves each result to a full Product entity via the "product_id"
     * key stored in document metadata at index time.
     *
     * SearchRequest API change in Spring AI 1.0.0 GA:
     * BROKEN (milestone API): SearchRequest.query(query).withTopK(5)
     * CORRECT (GA API): SearchRequest.builder().query(query).topK(5).build()
     *
     * The static SearchRequest.query(String) factory method was removed in GA.
     * Using it produces: "cannot find symbol — method query(java.lang.String)".
     *
     * The dead queryVector variable that was making a redundant embed() call
     * on every search has also been removed — VectorStore handles its own
     * query embedding internally.
     */
    public List<Product> semanticSearch(String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build());

        return docs.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();
                    Object rawId = metadata.get("product_id");
                    if (rawId == null)
                        return null;
                    Long productId = Long.valueOf(rawId.toString());
                    return productRepository.findById(productId).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}