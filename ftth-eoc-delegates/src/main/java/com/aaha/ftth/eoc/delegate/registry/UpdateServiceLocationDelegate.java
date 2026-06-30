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
 * Delegate that updates the service location in the Service Registry when a
 * Service Move order is fulfilled. Updates the physical connection details
 * (port, OLT, splitter, pincode) to reflect the new service location.
 *
 * If the registeredServiceId is not available in the process context, the delegate
 * falls back to searching for the service using the customer code.
 */
@Component("updateServiceLocationDelegate")
public class UpdateServiceLocationDelegate extends AbstractEocDelegate {

    private final ServiceRegistryService serviceRegistryService;
    private final EocDelegateConfig config;

    @Autowired
    public UpdateServiceLocationDelegate(ServiceRegistryService serviceRegistryService,
                                         EocDelegateConfig config) {
        this.serviceRegistryService = serviceRegistryService;
        this.config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Attempt to read registeredServiceId; fall back to search if missing
            String serviceId = resolveServiceId(execution);

            // Read required new location variables
            String newPincode = getRequiredString(execution, "newPincode");
            String newPortId = getRequiredString(execution, "newPortId");
            String newOltCode = getRequiredString(execution, "newOltCode");
            String newSplitterNumber = getRequiredString(execution, "newSplitterNumber");

            // Build update payload with new location details
            Map<String, Object> payload = buildUpdatePayload(newPortId, newOltCode, newSplitterNumber, newPincode);

            // Invoke service registry to update the service
            serviceRegistryService.updateService(serviceId, payload);

        } catch (BpmnError e) {
            // Re-throw BpmnErrors (e.g., ERR_MISSING_VARIABLE) without wrapping
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_SERVICE_LOCATION_UPDATE_FAILED",
                "Service location update failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveServiceId(DelegateExecution execution) {
        // Try to read registeredServiceId directly
        Object serviceIdObj = getOptionalVariable(execution, "registeredServiceId");
        if (serviceIdObj != null) {
            return serviceIdObj.toString();
        }

        // Fallback: search using customerCode
        String customerCode = getRequiredString(execution, "customerCode");

        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("relatedParty.id", customerCode);

        Map<String, Object> searchResponse = serviceRegistryService.searchServices(searchCriteria);

        // Extract service ID from search results
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
        if (results == null || results.isEmpty()) {
            throw new BpmnError("ERR_MISSING_VARIABLE",
                "Could not find service for customer");
        }

        return (String) results.get(0).get("id");
    }

    private Map<String, Object> buildUpdatePayload(String newPortId, String newOltCode,
                                                    String newSplitterNumber, String newPincode) {
        Map<String, Object> payload = new HashMap<>();

        // Service specification
        Map<String, Object> serviceSpec = new HashMap<>();
        serviceSpec.put("id", config.getServiceSpecId());
        payload.put("serviceSpecification", serviceSpec);

        // Status
        payload.put("status", "active");

        // Service characteristics with new location details
        List<Map<String, Object>> characteristics = new ArrayList<>();
        characteristics.add(createCharacteristic("portId", newPortId));
        characteristics.add(createCharacteristic("oltCode", newOltCode));
        characteristics.add(createCharacteristic("splitterNumber", newSplitterNumber));
        characteristics.add(createCharacteristic("pincode", newPincode));
        payload.put("serviceCharacteristic", characteristics);

        return payload;
    }

    private Map<String, Object> createCharacteristic(String name, Object value) {
        Map<String, Object> characteristic = new HashMap<>();
        characteristic.put("name", name);
        characteristic.put("value", value);
        return characteristic;
    }
}
