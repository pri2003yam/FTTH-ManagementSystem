package com.aaha.ftth.eoc.delegate.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ServiceRegistryService;
import com.aaha.ftth.eoc.delegate.config.EocDelegateConfig;

/**
 * Delegate that updates an existing FTTH product in the Service Registry
 * when a plan change occurs. Reads the registered product ID from the execution
 * context; if not available, falls back to searching by customer code.
 * Updates the product with new plan details (newPlanId, planName, monthlyPrice, oltType).
 */
@Component("updateProductForPlanChangeDelegate")
public class UpdateProductForPlanChangeDelegate extends AbstractEocDelegate {

    private final ServiceRegistryService serviceRegistryService;
    private final EocDelegateConfig config;

    @Autowired
    public UpdateProductForPlanChangeDelegate(ServiceRegistryService serviceRegistryService,
                                              EocDelegateConfig config) {
        this.serviceRegistryService = serviceRegistryService;
        this.config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Step 1: Resolve the product ID (primary or fallback)
            String productId = resolveProductId(execution);

            // Step 2: Read new plan details (all required)
            String newPlanId = String.valueOf(getRequiredVariable(execution, "newPlanId", Object.class));
            String planName = getRequiredString(execution, "planName");
            String monthlyPrice = String.valueOf(getRequiredVariable(execution, "monthlyPrice", Object.class));
            String oltType = getRequiredString(execution, "oltType");

            // Step 3: Build the update payload
            Map<String, Object> payload = buildUpdatePayload(newPlanId, planName, monthlyPrice, oltType);

            // Step 4: Invoke the service registry adapter to update the product
            serviceRegistryService.updateProduct(productId, payload);
        } catch (BpmnError e) {
            // Re-throw BpmnErrors (missing variable, missing product) as-is
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_PRODUCT_UPDATE_FAILED",
                "Failed to update product in Service Registry: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveProductId(DelegateExecution execution) {
        // Attempt to read registeredProductId; if missing, fall back to search
        Object registeredProductId = getOptionalVariable(execution, "registeredProductId");
        if (registeredProductId != null) {
            return String.valueOf(registeredProductId);
        }

        // Fallback: search by customerCode
        String customerCode = getRequiredString(execution, "customerCode");
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("relatedParty.id", customerCode);

        Map<String, Object> searchResponse = serviceRegistryService.searchProducts(searchCriteria);

        // Extract productId from search results
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
        if (results == null || results.isEmpty()) {
            throw new BpmnError("ERR_MISSING_VARIABLE",
                "Could not find product for customer");
        }

        return (String) results.get(0).get("id");
    }

    private Map<String, Object> buildUpdatePayload(String newPlanId, String planName,
                                                    String monthlyPrice, String oltType) {
        // Product specification
        Map<String, Object> productSpec = new HashMap<>();
        productSpec.put("id", config.getProductSpecId());

        // Product characteristics with new plan details
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(createCharacteristic("planId", newPlanId));
        characteristics.add(createCharacteristic("planName", planName));
        characteristics.add(createCharacteristic("monthlyPrice", monthlyPrice));
        characteristics.add(createCharacteristic("oltType", oltType));

        // Build full payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("productSpecification", productSpec);
        payload.put("status", "active");
        payload.put("productCharacteristic", characteristics);

        return payload;
    }

    private Map<String, Object> createCharacteristic(String name, String value) {
        Map<String, Object> characteristic = new HashMap<>();
        characteristic.put("name", name);
        characteristic.put("value", value);
        return characteristic;
    }
}
