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
 * Camunda delegate that creates a Shopping Cart for the Service Move workflow.
 * Builds a TMF773 cart payload with action "modify" containing new location details
 * (pincode, port ID, OLT code) and the existing connection reference.
 */
@Component("createServiceMoveCartDelegate")
public class CreateServiceMoveCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;
    private final EocDelegateConfig config;

    @Autowired
    public CreateServiceMoveCartDelegate(ShoppingCartService shoppingCartService, EocDelegateConfig config) {
        this.shoppingCartService = shoppingCartService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String connectionId = getRequiredString(execution, "connectionId");
        String customerId = getRequiredString(execution, "customerId");
        String newPincode = getRequiredString(execution, "newPincode");
        String newPortId = getRequiredString(execution, "newPortId");
        String newOltCode = getRequiredString(execution, "newOltCode");

        Map<String, Object> payload = buildCartPayload(connectionId, customerId, newPincode, newPortId, newOltCode);

        try {
            Map<String, Object> result = shoppingCartService.createShoppingCart(payload);
            String shoppingCartId = (String) result.get("id");
            execution.setVariable("shoppingCartId", shoppingCartId);
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_CREATION_FAILED",
                "Failed to create service move shopping cart: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCartPayload(String connectionId, String customerId,
                                                  String newPincode, String newPortId, String newOltCode) {
        Map<String, Object> payload = new HashMap<>();

        // Build cart item
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("action", "modify");

        // Product offering
        Map<String, Object> productOffering = new HashMap<>();
        productOffering.put("id", config.getProductSpecId());
        productOffering.put("name", "FTTH Subscription");
        cartItem.put("productOffering", productOffering);

        // Product details
        Map<String, Object> product = new HashMap<>();

        // Product specification
        Map<String, Object> productSpec = new HashMap<>();
        productSpec.put("id", config.getProductSpecId());
        product.put("productSpecification", productSpec);

        // Existing connection reference
        Map<String, Object> existingConnection = new HashMap<>();
        existingConnection.put("id", connectionId);
        existingConnection.put("referenceType", "existingConnection");
        product.put("productRelationship", List.of(existingConnection));

        // Product characteristics with new location details
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(Map.of("name", "newPincode", "value", newPincode));
        characteristics.add(Map.of("name", "newPortId", "value", newPortId));
        characteristics.add(Map.of("name", "newOltCode", "value", newOltCode));
        product.put("productCharacteristic", characteristics);

        // Related party (customer)
        List<Map<String, Object>> relatedParties = new ArrayList<>();
        relatedParties.add(Map.of("id", customerId, "role", "customer"));
        product.put("relatedParty", relatedParties);

        cartItem.put("product", product);

        // Wrap in cartItem array
        payload.put("cartItem", List.of(cartItem));

        return payload;
    }
}
