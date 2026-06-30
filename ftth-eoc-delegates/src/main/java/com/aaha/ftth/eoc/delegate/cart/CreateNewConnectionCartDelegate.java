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
 * Delegate that creates a Shopping Cart for a new FTTH connection.
 * Builds a TMF773 cart payload with action "add" containing the FTTH plan,
 * customer details, and service area pincode, then invokes the shopping cart adapter.
 */
@Component("createNewConnectionCartDelegate")
public class CreateNewConnectionCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;
    private final EocDelegateConfig config;

    @Autowired
    public CreateNewConnectionCartDelegate(ShoppingCartService shoppingCartService,
                                           EocDelegateConfig config) {
        this.shoppingCartService = shoppingCartService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read required process variables
        String customerName = getRequiredString(execution, "customerName");
        String email = getRequiredString(execution, "email");
        String planId = String.valueOf(getRequiredVariable(execution, "planId", Object.class));
        String pincode = String.valueOf(getRequiredVariable(execution, "pincode", Object.class));
        String oltType = getRequiredString(execution, "oltType");
        String customerCode = getRequiredString(execution, "customerCode");

        // Build TMF773 Shopping Cart payload
        Map<String, Object> payload = buildCartPayload(customerName, email, planId, pincode, oltType, customerCode);

        try {
            // Invoke the shopping cart adapter
            Map<String, Object> response = shoppingCartService.createShoppingCart(payload);

            // Store the returned cart ID in the process variable
            String shoppingCartId = (String) response.get("id");
            execution.setVariable("shoppingCartId", shoppingCartId);
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_CREATION_FAILED",
                "Failed to create shopping cart for new connection: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCartPayload(String customerName, String email,
                                                  String planId, String pincode,
                                                  String oltType, String customerCode) {
        // Product characteristics
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(createCharacteristic("planId", planId));
        characteristics.add(createCharacteristic("oltType", oltType));
        characteristics.add(createCharacteristic("pincode", pincode));
        characteristics.add(createCharacteristic("customerName", customerName));
        characteristics.add(createCharacteristic("email", email));

        // Related party
        List<Map<String, Object>> relatedParties = new ArrayList<>();
        Map<String, Object> relatedParty = new HashMap<>();
        relatedParty.put("id", customerCode);
        relatedParty.put("role", "customer");
        relatedParties.add(relatedParty);

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
        cartItem.put("action", "add");
        cartItem.put("productOffering", productOffering);
        cartItem.put("product", product);

        // Cart payload
        List<Map<String, Object>> cartItems = new ArrayList<>();
        cartItems.add(cartItem);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cartItem", cartItems);

        return payload;
    }

    private Map<String, Object> createCharacteristic(String name, String value) {
        Map<String, Object> characteristic = new HashMap<>();
        characteristic.put("name", name);
        characteristic.put("value", value);
        return characteristic;
    }
}
