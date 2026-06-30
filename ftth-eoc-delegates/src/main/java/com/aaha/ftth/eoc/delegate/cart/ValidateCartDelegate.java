package com.aaha.ftth.eoc.delegate.cart;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;

/**
 * Delegate that validates the contents of a Shopping Cart against business rules.
 * Reads the shoppingCartId from the process context and invokes the validation adapter.
 * Sets cartValid to true on success, or cartValid to false with cartValidationErrors on failure.
 */
@Component("validateCartDelegate")
public class ValidateCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ValidateCartDelegate(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read the shopping cart ID from the process context
        String shoppingCartId = getRequiredString(execution, "shoppingCartId");

        try {
            // Invoke the shopping cart validation adapter
            Map<String, Object> response = shoppingCartService.validateShoppingCart(shoppingCartId);

            // Check the validation result
            Boolean valid = (Boolean) response.get("valid");
            if (Boolean.TRUE.equals(valid)) {
                execution.setVariable("cartValid", true);
            } else {
                execution.setVariable("cartValid", false);
                Object errors = response.get("errors");
                execution.setVariable("cartValidationErrors",
                        errors != null ? errors.toString() : "Validation failed with no error details");
            }
        } catch (BpmnError e) {
            // Re-throw BpmnError instances (e.g., from getRequiredString) unchanged
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_VALIDATION_FAILED",
                    "Failed to validate shopping cart: " + e.getMessage());
        }
    }
}
