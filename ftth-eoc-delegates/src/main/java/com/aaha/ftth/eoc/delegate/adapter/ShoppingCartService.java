package com.aaha.ftth.eoc.delegate.adapter;

import java.util.Map;

/**
 * Interface representing the EOC Process Controller's built-in shoppingCartService bean.
 * This adapter provides TMF773 Shopping Cart operations.
 *
 * Note: At runtime, the actual implementation is provided by the EOC Process Controller.
 * This interface exists to allow type-safe autowiring in delegate classes.
 */
public interface ShoppingCartService {

    /**
     * Creates a new Shopping Cart with the given payload.
     *
     * @param payload the cart creation payload (TMF773 compliant)
     * @return a Map containing the created cart details, including "id" key with the cart identifier
     */
    Map<String, Object> createShoppingCart(Map<String, Object> payload);

    /**
     * Validates the contents of an existing Shopping Cart.
     *
     * @param cartId the Shopping Cart identifier
     * @return the validation result as a Map
     */
    Map<String, Object> validateShoppingCart(String cartId);

    /**
     * Triggers pricing calculation for a Shopping Cart.
     *
     * @param cartId the Shopping Cart identifier
     * @return the priced cart response as a Map
     */
    Map<String, Object> priceShoppingCart(String cartId);

    /**
     * Submits a Shopping Cart for fulfillment.
     *
     * @param cartId the Shopping Cart identifier
     * @return the submission result containing order reference as a Map
     */
    Map<String, Object> submitShoppingCart(String cartId);
}
