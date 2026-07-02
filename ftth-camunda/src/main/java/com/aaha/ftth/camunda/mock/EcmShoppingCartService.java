package com.aaha.ftth.camunda.mock;

import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Real implementation of ShoppingCartService that integrates with ECM.
 * 
 * Flow:
 *  1. CREATE  → Generates cart ID, stores payload context
 *  2. VALIDATE → Calls backend to validate service area + port availability
 *  3. PRICE   → Calls ECM (via backend) to get real charges from product offering
 *  4. SUBMIT  → Generates order reference for fulfillment
 *
 * This replaces MockShoppingCartService when ECM is available.
 * The backend exposes /api/ecm/offerings/{itemCode} which returns charges from ECM DB.
 */
@Component
public class EcmShoppingCartService implements ShoppingCartService {

    private static final Logger log = LoggerFactory.getLogger(EcmShoppingCartService.class);

    @Value("${ftth.backend.base-url:http://localhost:8085}")
    private String backendBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // In-memory cart store (for process lifetime)
    private final Map<String, Map<String, Object>> cartStore = new HashMap<>();

    // Maps MySQL plan names to ECM item codes (your 4 POs)
    private static final Map<String, String> PLAN_NAME_TO_ECM = Map.of(
        "Basic",    "FTTH_BASIC_50",
        "Standard", "FTTH_STD_100",
        "Premium",  "FTTH_PREM_300",
        "Ultra",    "FTTH_ULTRA_500"
    );

    @Override
    public Map<String, Object> createShoppingCart(Map<String, Object> payload) {
        String cartId = "CART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("=== [ECM] Shopping Cart CREATED ===");
        log.info("  Cart ID: {}", cartId);
        log.info("  Payload: {}", payload);

        // Store cart payload for use in validate/price steps
        cartStore.put(cartId, payload);

        Map<String, Object> response = new HashMap<>();
        response.put("id", cartId);
        response.put("status", "created");
        return response;
    }

    @Override
    public Map<String, Object> validateShoppingCart(String cartId) {
        log.info("=== [ECM] Shopping Cart VALIDATE ===");
        log.info("  Cart ID: {}", cartId);

        Map<String, Object> response = new HashMap<>();

        Map<String, Object> cartPayload = cartStore.get(cartId);
        if (cartPayload == null) {
            // If no stored payload, still pass validation (delegate will handle errors)
            response.put("valid", true);
            return response;
        }

        // Extract characteristics from TMF773 cart payload
        String pincode = extractCharacteristic(cartPayload, "pincode");
        String oltType = extractCharacteristic(cartPayload, "oltType");

        if (pincode != null && oltType != null) {
            // Call backend to check service area + port availability
            try {
                String url = backendBaseUrl + "/api/inventory/olts?pincode=" + pincode;
                ResponseEntity<List> resp = restTemplate.getForEntity(url, List.class);

                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> olts = resp.getBody();
                    boolean hasPort = olts.stream().anyMatch(o ->
                        oltType.equals(o.get("oltType")) &&
                        ((Number) o.getOrDefault("availablePorts", 0)).intValue() > 0
                    );

                    if (!hasPort) {
                        response.put("valid", false);
                        response.put("errors", "No " + oltType + " ports available in pincode " + pincode);
                        log.info("  VALIDATION FAILED: No {} ports in pincode {}", oltType, pincode);
                        return response;
                    }
                }
            } catch (Exception e) {
                log.warn("  Validation check failed (non-blocking): {}", e.getMessage());
                // Non-blocking — if backend is down, pass validation
            }
        }

        response.put("valid", true);
        log.info("  VALIDATION PASSED");
        return response;
    }

    @Override
    public Map<String, Object> priceShoppingCart(String cartId) {
        log.info("=== [ECM] Shopping Cart PRICE ===");
        log.info("  Cart ID: {}", cartId);

        Map<String, Object> response = new HashMap<>();
        double totalPrice = 0.0;

        Map<String, Object> cartPayload = cartStore.get(cartId);
        String planId = cartPayload != null ? extractCharacteristic(cartPayload, "planId") : null;
        String planName = cartPayload != null ? extractCharacteristic(cartPayload, "planName") : null;

        // Resolve ECM item code from plan name (e.g. "Ultra" -> "FTTH_ULTRA_500")
        String ecmItemCode = planName != null ? PLAN_NAME_TO_ECM.get(planName) : null;
        String lookupCode = ecmItemCode != null ? ecmItemCode : planId;

        // Try to get price from ECM via backend
        if (lookupCode != null) {
            try {
                String url = backendBaseUrl + "/api/ecm/offerings/" + lookupCode;
                ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);

                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> offering = resp.getBody();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> charges = (List<Map<String, Object>>) offering.get("charges");

                    if (charges != null) {
                        for (Map<String, Object> charge : charges) {
                            Object val = charge.get("value");
                            if (val instanceof Number) {
                                totalPrice += ((Number) val).doubleValue();
                            }
                        }
                    }
                    log.info("  ECM pricing from offering '{}': totalPrice={}", offering.get("name"), totalPrice);
                }
            } catch (Exception e) {
                log.warn("  ECM pricing failed, falling back to plan DB price: {}", e.getMessage());
            }
        }

        // Fallback: if ECM didn't return pricing, use plan's monthly price from DB
        if (totalPrice == 0.0 && planId != null) {
            try {
                String url = backendBaseUrl + "/api/plans/" + planId;
                ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Object price = resp.getBody().get("monthlyPrice");
                    if (price instanceof Number) {
                        totalPrice = ((Number) price).doubleValue();
                    }
                }
            } catch (Exception e) {
                log.warn("  Plan price lookup failed: {}", e.getMessage());
                totalPrice = 999.0; // Last resort fallback
            }
        }

        if (totalPrice == 0.0) {
            totalPrice = 999.0; // Safety fallback
        }

        response.put("totalPrice", totalPrice);
        response.put("currency", "INR");
        log.info("  Final price: {}", totalPrice);
        return response;
    }

    @Override
    public Map<String, Object> submitShoppingCart(String cartId) {
        String orderRef = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("=== [ECM] Shopping Cart SUBMITTED ===");
        log.info("  Cart ID: {}", cartId);
        log.info("  Order Reference: {}", orderRef);

        // Clean up stored cart
        cartStore.remove(cartId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderReference", orderRef);
        response.put("status", "submitted");
        return response;
    }

    /**
     * Extract a characteristic value from TMF773 cart payload.
     * Structure: { cartItem: [{ product: { productCharacteristic: [{ name, value }] } }] }
     */
    @SuppressWarnings("unchecked")
    private String extractCharacteristic(Map<String, Object> payload, String charName) {
        try {
            List<Map<String, Object>> cartItems = (List<Map<String, Object>>) payload.get("cartItem");
            if (cartItems == null || cartItems.isEmpty()) return null;

            Map<String, Object> product = (Map<String, Object>) cartItems.get(0).get("product");
            if (product == null) return null;

            List<Map<String, Object>> characteristics =
                (List<Map<String, Object>>) product.get("productCharacteristic");
            if (characteristics == null) return null;

            for (Map<String, Object> c : characteristics) {
                if (charName.equals(c.get("name"))) {
                    Object val = c.get("value");
                    return val != null ? val.toString() : null;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract characteristic '{}': {}", charName, e.getMessage());
        }
        return null;
    }
}
