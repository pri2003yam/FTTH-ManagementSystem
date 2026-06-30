package com.aaha.ftth.eoc.delegate.cart;

import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ShoppingCartService;

/**
 * Delegate that submits a Shopping Cart to trigger fulfillment in EOC.
 * Reads the shoppingCartId from the process context and invokes the submission adapter.
 * Stores the returned order reference in the orderReference process variable.
 */
@Component("submitCartDelegate")
public class SubmitCartDelegate extends AbstractEocDelegate {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public SubmitCartDelegate(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read the shopping cart ID from the process context
        String shoppingCartId = getRequiredString(execution, "shoppingCartId");

        try {
            // Invoke the shopping cart submission adapter
            Map<String, Object> response = shoppingCartService.submitShoppingCart(shoppingCartId);

            // Extract the order reference from the response and store it
            Object orderRef = response.get("orderReference");
            execution.setVariable("orderReference",
                    orderRef != null ? orderRef.toString() : null);
        } catch (BpmnError e) {
            // Re-throw BpmnError instances (e.g., from getRequiredString) unchanged
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_CART_SUBMISSION_FAILED",
                    "Failed to submit shopping cart: " + e.getMessage());
        }
    }
}
