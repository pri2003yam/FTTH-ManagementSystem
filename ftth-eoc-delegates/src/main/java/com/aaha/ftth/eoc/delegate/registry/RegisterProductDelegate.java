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
 * Delegate that registers a new FTTH product in the Service Registry after
 * successful fulfillment of a New Connection order.
 * Builds a product payload with plan details, customer reference, and status "active",
 * then invokes the service registry adapter to create the product.
 */
@Component("registerProductDelegate")
public class RegisterProductDelegate extends AbstractEocDelegate {

    private final ServiceRegistryService serviceRegistryService;
    private final EocDelegateConfig config;

    @Autowired
    public RegisterProductDelegate(ServiceRegistryService serviceRegistryService,
                                   EocDelegateConfig config) {
        this.serviceRegistryService = serviceRegistryService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Read required process variables
        String planId = String.valueOf(getRequiredVariable(execution, "planId", Object.class));
        String planName = getRequiredString(execution, "planName");
        String monthlyPrice = String.valueOf(getRequiredVariable(execution, "monthlyPrice", Object.class));
        String oltType = getRequiredString(execution, "oltType");
        String customerCode = getRequiredString(execution, "customerCode");

        // Build the product payload
        Map<String, Object> payload = buildProductPayload(planId, planName, monthlyPrice, oltType, customerCode);

        try {
            // Invoke the service registry adapter to create the product
            Map<String, Object> response = serviceRegistryService.createProduct(payload);

            // Store the returned product ID in the process variable
            String registeredProductId = (String) response.get("id");
            execution.setVariable("registeredProductId", registeredProductId);
        } catch (Exception e) {
            throw new BpmnError("ERR_PRODUCT_REGISTRATION_FAILED",
                "Failed to register product in Service Registry: " + e.getMessage());
        }
    }

    private Map<String, Object> buildProductPayload(String planId, String planName,
                                                     String monthlyPrice, String oltType,
                                                     String customerCode) {
        // Product specification
        Map<String, Object> productSpec = new HashMap<>();
        productSpec.put("id", config.getProductSpecId());

        // Product characteristics
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(createCharacteristic("planId", planId));
        characteristics.add(createCharacteristic("planName", planName));
        characteristics.add(createCharacteristic("monthlyPrice", monthlyPrice));
        characteristics.add(createCharacteristic("oltType", oltType));

        // Related party (EOC uses "relatedParties" with "reference" field)
        List<Map<String, Object>> relatedParties = new ArrayList<>();
        Map<String, Object> relatedParty = new HashMap<>();
        relatedParty.put("reference", customerCode);
        relatedParty.put("role", "Customer");
        relatedParties.add(relatedParty);

        // Build full payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "FTTH-" + planName + "-" + customerCode);
        payload.put("productSerialNumber", "PSN-" + customerCode + "-" + System.currentTimeMillis());
        payload.put("productSpecification", productSpec);
        payload.put("status", "active");
        payload.put("validFor", Map.of("start", java.time.Instant.now().toString()));
        payload.put("productCharacteristic", characteristics);
        payload.put("relatedParties", relatedParties);

        return payload;
    }

    private Map<String, Object> createCharacteristic(String name, String value) {
        Map<String, Object> characteristic = new HashMap<>();
        characteristic.put("name", name);
        characteristic.put("value", value);
        return characteristic;
    }
}
