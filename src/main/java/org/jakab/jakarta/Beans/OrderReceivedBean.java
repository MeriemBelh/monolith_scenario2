package org.jakab.jakarta.Beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import org.jakab.jakarta.domain.Order;
import org.jakab.jakarta.service.OrderService;
import org.jakab.jakarta.service.SessionHandlerFactory;
import org.jakab.jakarta.service.SessionHandlerService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Named("orderReceived")
@RequestScoped
public class OrderReceivedBean {

    private Map<Integer, String> itemQuantities;

    @Inject
    private OrderService service;

    @Inject
    private SessionHandlerService handler;

    @PostConstruct
    public void init() {
        handler = SessionHandlerFactory.getHandler();
        itemQuantities = new HashMap<>();
    }

    public String placeOrder() {
        try {
            // Convert itemQuantities to JSON format
            String itemsJson = convertToJson(itemQuantities);
            System.out.println("Order JSON: " + itemsJson);

            // Proceed with placing the order and handling the response
            Order order = service.placeOrder(itemQuantities);
            handler.newOrder(order);

            // Store order ID in session for retrieval in the React app
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
            session.setAttribute("orderId", order.getId());

            // Construct URL to redirect to React SPA with order details
            String reactAppUrl = "http://localhost:3000/order";
            String queryParams = "?items=" + URLEncoder.encode(itemsJson, "UTF-8");
            facesContext.getExternalContext().redirect(reactAppUrl + queryParams);
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions
        }
        return null; // Navigation case handled by redirect
    }

    private String convertToJson(Map<Integer, String> itemQuantities) {
        Map<String, Map<Integer, String>> jsonMap = new HashMap<>();
        Map<Integer, String> filteredItems = new HashMap<>();

        // Filter items with quantity > 0
        for (Map.Entry<Integer, String> entry : itemQuantities.entrySet()) {
            Integer itemId = entry.getKey();
            String quantity = entry.getValue();
            if (!quantity.isEmpty() && Integer.parseInt(quantity) > 0) {
                filteredItems.put(itemId, quantity);
            }
        }

        jsonMap.put("items", filteredItems);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters and setters

    public Map<Integer, String> getItemQuantities() {
        return itemQuantities;
    }

    public void setItemQuantities(Map<Integer, String> itemQuantities) {
        this.itemQuantities = itemQuantities;
    }
}
