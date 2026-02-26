package com.retail.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.retail.dto.InventoryDataDTO;
import com.retail.model.Inventory;
import com.retail.model.Product;
import com.retail.repository.InventoryRepository;
import com.retail.repository.OrderRepository;

@Service
public class AIService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private InventoryRepository inventoryRepo;

    @Autowired
    private VectorStore vectorStore;

    public List<Product> semanticSearch(String query) {
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(10)
                .withSimilarityThreshold(0.7);

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream()
                .map(this::convertToProduct)
                .collect(Collectors.toList());
    }

    public String generateStockForecast() {
        List<InventoryDataDTO> data = prepareForecastingData();

        String prompt = "You are a retail inventory expert. Analyse this data: " + data.toString()
                + ". Predict stockouts for the next 30 days and suggest restock quantities.";
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private List<InventoryDataDTO> prepareForecastingData() {
        List<Inventory> allInventory = inventoryRepo.findAll();
        List<InventoryDataDTO> dataList = new ArrayList<>();

        for (Inventory inv : allInventory) {
            // For a real app, you'd aggregate sales by product from orderRepo
            dataList.add(new InventoryDataDTO(inv.getProduct().getName(), inv.getQuantity(), new ArrayList<>()));
        }
        return dataList;
    }

    private Product convertToProduct(Document doc) {
        // In RAG, metadata typically contains the original object ID or details
        Product p = new Product();
        p.setName(doc.getContent()); // Simplification
        return p;
    }
}
