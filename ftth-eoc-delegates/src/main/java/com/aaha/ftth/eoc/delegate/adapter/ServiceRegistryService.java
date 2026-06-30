package com.aaha.ftth.eoc.delegate.adapter;

import java.util.Map;

/**
 * Interface representing the EOC Process Controller's built-in serviceRegistryService bean.
 * This adapter provides CRUD operations for products and services in the Service Registry.
 *
 * Note: At runtime, the actual implementation is provided by the EOC Process Controller.
 * This interface exists to allow type-safe autowiring in delegate classes.
 */
public interface ServiceRegistryService {

    /**
     * Creates a new product in the Service Registry.
     *
     * @param payload the product creation payload
     * @return a Map containing the created product details, including "id" key with the product identifier
     */
    Map<String, Object> createProduct(Map<String, Object> payload);

    /**
     * Retrieves a product by its identifier.
     *
     * @param productId the product identifier
     * @return a Map containing the product details
     */
    Map<String, Object> getProductById(String productId);

    /**
     * Updates an existing product (full replacement).
     *
     * @param productId the product identifier
     * @param payload the updated product payload
     * @return a Map containing the updated product details
     */
    Map<String, Object> updateProduct(String productId, Map<String, Object> payload);

    /**
     * Partially updates an existing product (patch).
     *
     * @param productId the product identifier
     * @param payload the partial update payload
     * @return a Map containing the patched product details
     */
    Map<String, Object> patchProduct(String productId, Map<String, Object> payload);

    /**
     * Deletes a product from the Service Registry.
     *
     * @param productId the product identifier
     * @return a Map containing the deletion result
     */
    Map<String, Object> deleteProduct(String productId);

    /**
     * Searches for products matching the given criteria.
     *
     * @param searchCriteria the search filter parameters
     * @return a Map containing the search results
     */
    Map<String, Object> searchProducts(Map<String, Object> searchCriteria);

    /**
     * Creates a new service in the Service Registry.
     *
     * @param payload the service creation payload
     * @return a Map containing the created service details, including "id" key with the service identifier
     */
    Map<String, Object> createService(Map<String, Object> payload);

    /**
     * Retrieves a service by its identifier.
     *
     * @param serviceId the service identifier
     * @return a Map containing the service details
     */
    Map<String, Object> getServiceById(String serviceId);

    /**
     * Updates an existing service (full replacement).
     *
     * @param serviceId the service identifier
     * @param payload the updated service payload
     * @return a Map containing the updated service details
     */
    Map<String, Object> updateService(String serviceId, Map<String, Object> payload);

    /**
     * Partially updates an existing service (patch).
     *
     * @param serviceId the service identifier
     * @param payload the partial update payload
     * @return a Map containing the patched service details
     */
    Map<String, Object> patchService(String serviceId, Map<String, Object> payload);

    /**
     * Deletes a service from the Service Registry.
     *
     * @param serviceId the service identifier
     * @return a Map containing the deletion result
     */
    Map<String, Object> deleteService(String serviceId);

    /**
     * Searches for services matching the given criteria.
     *
     * @param searchCriteria the search filter parameters
     * @return a Map containing the search results
     */
    Map<String, Object> searchServices(Map<String, Object> searchCriteria);
}
