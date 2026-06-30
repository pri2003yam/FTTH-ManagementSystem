package com.aaha.ftth.eoc.delegate.cart;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;

/**
 * Delegate that triggers pricing calculation for a Shopping Cart.
 * Reads the shoppingCartId from the process context, invokes the pricing adapter,
 * and stores the total price in the cartTotalPrice process variable.
 */
@Component("priceCartDelegate")
public class PriceCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public PriceCartDelegate(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read the shopping cart ID from the process context
        String shoppingCartId = getRequiredString(execution, "shoppingCartId");

        try {
            // Invoke the shopping cart pricing adapter
            Map<String, Object> response = shoppingCartService.priceShoppingCart(shoppingCartId);

            // Extract total price from the response and store in process variable
            Object totalPrice = response.get("totalPrice");
            if (totalPrice instanceof Number) {
                execution.setVariable("cartTotalPrice", ((Number) totalPrice).doubleValue());
            } else {
                execution.setVariable("cartTotalPrice", totalPrice);
            }
        } catch (BpmnError e) {
            // Re-throw BpmnError instances (e.g., from getRequiredString) unchanged
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_PRICING_FAILED",
                    "Failed to price shopping cart: " + e.getMessage());
        }
    }
}
