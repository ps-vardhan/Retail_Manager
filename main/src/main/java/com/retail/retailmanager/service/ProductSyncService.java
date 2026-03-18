package com.retail.retailmanager.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.retail.retailmanager.model.Inventory;
import com.retail.retailmanager.model.Product;
import com.retail.retailmanager.repository.InventoryRepository;
import com.retail.retailmanager.repository.ProductRepository;

@Service
public class ProductSyncService {

    private final WebClient webClient;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final EmbeddingService embeddingService;

    public ProductSyncService(
            @Value("${product.api.base-url}") String baseUrl,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            EmbeddingService embeddingService) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Fetches up to 100 products from DummyJSON and upserts each one.
     *
     * Three sequential steps per product — all three are required:
     *
     * 1. productRepository.save()
     * Persists the product row and generates the DB id.
     * Without the id, the embedding metadata and inventory FK are both broken.
     *
     * 2. embeddingService.generateAndStore()
     * Writes float[1536] to product.embedding (raw column) AND adds a Document
     * to the Spring AI vector_store table. Without this, similaritySearch()
     * always returns zero results.
     *
     * 3. inventory upsert ← THIS WAS MISSING
     * The original sync created Product rows with no corresponding Inventory row.
     * OrderController.placeOrder() calls inventoryRepository.findByProductId()
     * and throws RuntimeException("Inventory not found") for every synced product
     * because no Inventory row exists. Every Order button click produced a 500.
     * DummyJSON provides a "stock" field — use it as the initial quantity.
     * If "stock" is absent, default to 0 (product visible but not orderable).
     * Upsert: if an Inventory row already exists for this product, update the
     * quantity rather than inserting a duplicate (idempotent sync).
     */
    @SuppressWarnings("unchecked")
    public void syncAllProducts() {
        Map<String, Object> response = webClient.get()
                .uri("/products?limit=100&skip=0")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("products")) {
            throw new RuntimeException("DummyJSON returned an unexpected response shape");
        }

        List<Map<String, Object>> rawProducts = (List<Map<String, Object>>) response.get("products");

        for (Map<String, Object> raw : rawProducts) {
            String title = (String) raw.get("title");

            // Step 1 — upsert product row
            Product product = productRepository.findByName(title)
                    .orElseGet(Product::new);

            product.setName(title);
            product.setDescription((String) raw.get("description"));
            product.setCategory((String) raw.get("category"));
            product.setThumbnailUrl((String) raw.get("thumbnail"));

            Number priceRaw = (Number) raw.get("price");
            product.setPrice(BigDecimal.valueOf(priceRaw.doubleValue()));

            productRepository.save(product);

            // Step 2 — generate embedding and write to both product.embedding
            // column and the Spring AI vector_store table
            embeddingService.generateAndStore(product);

            // Step 3 — upsert inventory row using DummyJSON "stock" field
            // inventoryRepository.findByProductId() reuses the existing row if
            // this is a repeat sync, preventing duplicate inventory rows.
            int stock = 0;
            Object stockRaw = raw.get("stock");
            if (stockRaw instanceof Number) {
                stock = ((Number) stockRaw).intValue();
            }

            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseGet(Inventory::new);
            inventory.setProduct(product);
            inventory.setQuantity(stock);
            inventoryRepository.save(inventory);
        }
    }
}