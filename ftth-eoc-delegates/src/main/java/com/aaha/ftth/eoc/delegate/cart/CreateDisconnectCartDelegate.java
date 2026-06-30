package com.aaha.ftth.eoc.delegate.cart;

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
 * Camunda delegate that creates a Shopping Cart for a disconnect (delete) operation.
 * Reads connectionId and customerId from the process, builds a TMF773 payload
 * with action "delete", and invokes the shoppingCartService adapter.
 */
@Component("createDisconnectCartDelegate")
public class CreateDisconnectCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;
    private final EocDelegateConfig config;

    @Autowired
    public CreateDisconnectCartDelegate(ShoppingCartService shoppingCartService,
                                        EocDelegateConfig config) {
        this.shoppingCartService = shoppingCartService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String connectionId = getRequiredString(execution, "connectionId");
        String customerId = getRequiredString(execution, "customerId");

        try {
            Map<String, Object> payload = buildCartPayload(connectionId, customerId);
            Map<String, Object> result = shoppingCartService.createShoppingCart(payload);
            String shoppingCartId = (String) result.get("id");
            execution.setVariable("shoppingCartId", shoppingCartId);
        } catch (BpmnError e) {
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_CREATION_FAILED",
                "Failed to create disconnect shopping cart: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCartPayload(String connectionId, String customerId) {
        Map<String, Object> productSpec = new HashMap<>();
        productSpec.put("id", config.getProductSpecId());

        Map<String, Object> product = new HashMap<>();
        product.put("productSpecification", productSpec);
        product.put("id", connectionId);
        product.put("relatedParty", List.of(Map.of("id", customerId, "role", "customer")));

        Map<String, Object> productOffering = new HashMap<>();
        productOffering.put("id", config.getProductSpecId());
        productOffering.put("name", "FTTH Subscription");

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("action", "delete");
        cartItem.put("productOffering", productOffering);
        cartItem.put("product", product);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cartItem", List.of(cartItem));

        return payload;
    }
}
