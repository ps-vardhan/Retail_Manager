package com.retail.retailmanager.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.retail.retailmanager.model.Product;
import com.retail.retailmanager.repository.ProductRepository;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    public EmbeddingService(EmbeddingModel embeddingModel,
            ProductRepository productRepository,
            VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Two distinct writes happen here — both are required for the full pipeline:
     *
     * Write 1 — raw DB column:
     * Spring AI 1.0.0 GA: EmbeddingModel.embed(String) returns float[] directly.
     * The milestone API returned List<Double> — that overload no longer exists in
     * GA.
     * The previous code tried to assign float[] to List<Double>, which is the
     * exact compile error: "incompatible types: float[] cannot be converted to
     * java.util.List<java.lang.Double>".
     * The conversion loop is now gone — embed() hands back float[] ready to store.
     *
     * Write 2 — Spring AI vector_store table:
     * vectorStore.add() populates the separate pgvector-backed table that
     * AIService.semanticSearch() queries via similaritySearch().
     * Without this call that table is always empty and search always returns zero
     * results.
     * The "product_id" metadata key must match what AIService reads.
     *
     * Called by ProductSyncService after every productRepository.save()
     * and by AdminController after every manual product add.
     */
    public void generateAndStore(Product product) {
        String text = buildEmbeddingText(product);

        // Write 1: raw vector(1536) column on the products table.
        // embed(String) returns float[] in Spring AI 1.0.0 GA. No conversion needed.
        float[] embedding = embeddingModel.embed(text);
        product.setEmbedding(embedding);
        productRepository.save(product);

        // Write 2: Spring AI vector_store table so similaritySearch() returns results.
        Document doc = new Document(
                text,
                Map.of("product_id", product.getId().toString()));
        vectorStore.add(List.of(doc));
    }

    /**
     * Constructs the text passed to the embedding model.
     * Null-safe: skips fields that are null to avoid "null" literals in the string.
     */
    private String buildEmbeddingText(Product product) {
        StringBuilder sb = new StringBuilder();
        if (product.getName() != null) {
            sb.append(product.getName()).append(". ");
        }
        if (product.getDescription() != null) {
            sb.append(product.getDescription()).append(". ");
        }
        if (product.getCategory() != null) {
            sb.append("Category: ").append(product.getCategory());
        }
        return sb.toString().trim();
    }
}