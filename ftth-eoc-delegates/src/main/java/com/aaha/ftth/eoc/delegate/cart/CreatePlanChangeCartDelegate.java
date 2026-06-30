package com.aaha.ftth.eoc.delegate.cart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;
import com.aaha.ftth.eoc.delegate.config.EocDelegateConfig;

/**
 * Camunda delegate that creates a Shopping Cart for the Plan Change workflow.
 * Builds a TMF773 cart payload with action "modify" and the new plan reference,
 * then invokes the shoppingCartService adapter to create the cart.
 */
@Component("createPlanChangeCartDelegate")
public class CreatePlanChangeCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;
    private final EocDelegateConfig config;

    @Autowired
    public CreatePlanChangeCartDelegate(ShoppingCartService shoppingCartService, EocDelegateConfig config) {
        this.shoppingCartService = shoppingCartService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read required process variables
        String connectionId = getRequiredString(execution, "connectionId");
        String customerId = getRequiredString(execution, "customerId");
        Long newPlanId = getRequiredLong(execution, "newPlanId");

        // Build the cart payload
        Map<String, Object> payload = buildCartPayload(connectionId, customerId, newPlanId);

        try {
            // Invoke the shopping cart adapter
            Map<String, Object> result = shoppingCartService.createShoppingCart(payload);

            // Store the returned cart ID in the process variable
            String shoppingCartId = (String) result.get("id");
            execution.setVariable("shoppingCartId", shoppingCartId);
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_CREATION_FAILED",
                "Failed to create plan change shopping cart: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCartPayload(String connectionId, String customerId, Long newPlanId) {
        // Product characteristics
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(Map.of("name", "newPlanId", "value", String.valueOf(newPlanId)));
        characteristics.add(Map.of("name", "connectionId", "value", connectionId));

        // Related party
        List<Map<String, Object>> relatedParties = new ArrayList<>();
        relatedParties.add(Map.of("id", customerId, "role", "customer"));

        // Product specification
        Map<String, Object> productSpec = new HashMap<>();
        productSpec.put("id", config.getProductSpecId());

        // Product
        Map<String, Object> product = new HashMap<>();
        product.put("productSpecification", productSpec);
        product.put("productCharacteristic", characteristics);
        product.put("relatedParty", relatedParties);

        // Product offering
        Map<String, Object> productOffering = new HashMap<>();
        productOffering.put("id", config.getProductSpecId());
        productOffering.put("name", "FTTH Subscription");

        // Cart item
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("action", "modify");
        cartItem.put("productOffering", productOffering);
        cartItem.put("product", product);

        // Full payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("cartItem", List.of(cartItem));

        return payload;
    }
}
