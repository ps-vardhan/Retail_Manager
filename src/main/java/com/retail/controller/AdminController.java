package com.retail.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.retail.dto.FlipkartConfig;
import com.retail.service.AIService;
import com.retail.service.FlipkartService;
import com.retail.service.InventoryService;
import com.retail.service.OrderService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private FlipkartService flipkartService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AIService aiService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("listInventory", inventoryService.getAllInventory());
        model.addAttribute("listOrders", orderService.getAllOrders());

        try {
            String aiForecast = aiService.generateStockForecast();
            model.addAttribute("stockForecast", aiForecast);
        } catch (Exception e) {
            model.addAttribute("stockForecast", "AI Insights currently unavailable.");
        }
        return "admin/dashboard";
    }

    @GetMapping("/api-settings")
    public String apiSettings(Model model) {
        model.addAttribute("flipkartConfig", new FlipkartConfig());
        return "admin/api_settings";
    }

    @PostMapping("/api-settings/save")
    public String saveSettings(@ModelAttribute FlipkartConfig config) {
        return "redirect:/admin/api-settings?success";
    }

    @PostMapping("/products/sync")
    public String syncProducts() {
        flipkartService.syncAllProducts();
        return "redirect:/admin/dashboard?syncStarted";
    }
}
