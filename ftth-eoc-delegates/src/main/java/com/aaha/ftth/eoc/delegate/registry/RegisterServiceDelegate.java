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
 * Delegate that registers a new FTTH service in the EOC Service Registry.
 * Creates a service with physical connection characteristics (port, OLT, splitter, pincode)
 * linked to the previously registered product.
 */
@Component("registerServiceDelegate")
public class RegisterServiceDelegate extends AbstractEocDelegate {

    private final ServiceRegistryService serviceRegistryService;
    private final EocDelegateConfig config;

    @Autowired
    public RegisterServiceDelegate(ServiceRegistryService serviceRegistryService, EocDelegateConfig config) {
        this.serviceRegistryService = serviceRegistryService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Read required variables from process context
            String portId = getRequiredString(execution, "portId");
            String oltCode = getRequiredString(execution, "oltCode");
            String splitterNumber = getRequiredString(execution, "splitterNumber");
            String pincode = getRequiredString(execution, "pincode");
            String registeredProductId = getRequiredString(execution, "registeredProductId");
            String customerCode = getRequiredString(execution, "customerCode");

            // Build service payload
            Map<String, Object> payload = buildServicePayload(
                portId, oltCode, splitterNumber, pincode, registeredProductId, customerCode);

            // Invoke service registry to create the service
            Map<String, Object> response = serviceRegistryService.createService(payload);

            // Store the returned service ID in the process context
            execution.setVariable("registeredServiceId", response.get("id"));

        } catch (BpmnError e) {
            // Re-throw BpmnErrors (e.g., ERR_MISSING_VARIABLE) without wrapping
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_SERVICE_REGISTRATION_FAILED",
                "Service registration failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildServicePayload(
            String portId, String oltCode, String splitterNumber,
            String pincode, String registeredProductId, String customerCode) {

        Map<String, Object> payload = new HashMap<>();

        // Name (required by EOC)
        payload.put("name", "FTTH-SVC-" + customerCode + "-" + pincode);

        // Valid for (required by EOC)
        payload.put("validFor", Map.of("start", java.time.Instant.now().toString()));

        // Service specification
        Map<String, Object> serviceSpec = new HashMap<>();
        serviceSpec.put("id", config.getServiceSpecId());
        payload.put("serviceSpecification", serviceSpec);

        // Service characteristics
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(createCharacteristic("portId", portId));
        characteristics.add(createCharacteristic("oltCode", oltCode));
        characteristics.add(createCharacteristic("splitterNumber", splitterNumber));
        characteristics.add(createCharacteristic("pincode", pincode));
        payload.put("serviceCharacteristic", characteristics);

        // Supporting product
        List<Map<String, Object>> supportingProducts = new ArrayList<>();
        Map<String, Object> product = new HashMap<>();
        product.put("id", registeredProductId);
        supportingProducts.add(product);
        payload.put("supportingProduct", supportingProducts);

        // Related parties (EOC format: "relatedParties" with "reference")
        List<Map<String, Object>> relatedParties = new ArrayList<>();
        Map<String, Object> party = new HashMap<>();
        party.put("reference", customerCode);
        party.put("role", "Customer");
        relatedParties.add(party);
        payload.put("relatedParties", relatedParties);

        return payload;
    }

    private Map<String, Object> createCharacteristic(String name, Object value) {
        Map<String, Object> characteristic = new HashMap<>();
        characteristic.put("name", name);
        characteristic.put("value", value);
        return characteristic;
    }
}
