package com.aaha.ftth.camunda.mock;

import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of EOC's shoppingCartService bean for local testing.
 * DISABLED: Replaced by EcmShoppingCartService which integrates with real ECM.
 * To re-enable mocks, add @Component annotation back and remove EcmShoppingCartService.
 */
// @Component  ← DISABLED: Using EcmShoppingCartService instead
public class MockShoppingCartService implements ShoppingCartService {

    private static final Logger log = LoggerFactory.getLogger(MockShoppingCartService.class);

    @Override
    public Map<String, Object> createShoppingCart(Map<String, Object> payload) {
        String cartId = "CART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== [MOCK EOC] Shopping Cart CREATED ===");
        log.info("  Cart ID: {}", cartId);
        log.info("  Payload: {}", payload);
        Map<String, Object> response = new HashMap<>();
        response.put("id", cartId);
        response.put("status", "created");
        return response;
    }

    @Override
    public Map<String, Object> validateShoppingCart(String cartId) {
        log.info("=== [MOCK EOC] Shopping Cart VALIDATED ===");
        log.info("  Cart ID: {}", cartId);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("errors", null);
        return response;
    }

    @Override
    public Map<String, Object> priceShoppingCart(String cartId) {
        log.info("=== [MOCK EOC] Shopping Cart PRICED ===");
        log.info("  Cart ID: {}", cartId);
        Map<String, Object> response = new HashMap<>();
        response.put("totalPrice", 999.00);
        response.put("currency", "INR");
        return response;
    }

    @Override
    public Map<String, Object> submitShoppingCart(String cartId) {
        String orderRef = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== [MOCK EOC] Shopping Cart SUBMITTED ===");
        log.info("  Cart ID: {}", cartId);
        log.info("  Order Reference: {}", orderRef);
        Map<String, Object> response = new HashMap<>();
        response.put("orderReference", orderRef);
        response.put("status", "submitted");
        return response;
    }
}
