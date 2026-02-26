package com.retail.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.retail.model.Product;
import com.retail.service.AIService;
import com.retail.service.ProductService;

@Controller
public class ProductController {
    
    @Autowired
    private ProductService service;

    @Autowired
    private AIService aiService;

    @GetMapping("/products")
    public String viewHomePage(Model model) {
        model.addAttribute("listProducts", service.getAllProducts());
        return "products";
    }

    @GetMapping("/products/search")
    public String search(@RequestParam("q") String query, Model model) {
        List<Product> results = aiService.semanticSearch(query);
        model.addAttribute("listProducts", results);
        model.addAttribute("searchQuery", query);
        model.addAttribute("isSemantic", true);
        return "products";
    }

    @GetMapping("/showNewProductForm")
    public String showNewProductForm(Model model) {
        Product product = new Product();
        model.addAttribute("product", product);
        return "new_product";
    }

    @PostMapping("/saveProduct")
    public String saveProduct(Product product) {
        service.saveProduct(product);
        return "redirect:/products";
    }
}
