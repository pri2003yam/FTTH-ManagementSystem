package com.aaha.ftth.eoc.delegate.registry;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aaha.ftth.eoc.delegate.AbstractEocDelegate;
import com.aaha.ftth.eoc.delegate.adapter.ServiceRegistryService;

/**
 * Delegate that terminates an FTTH product and deactivates its associated service
 * in the Service Registry when a Disconnect order is fulfilled.
 *
 * The delegate patches the product status to "terminated" and the service status
 * to "inactive". If the registeredProductId is not directly available in the
 * process variables, it falls back to searching by customerCode.
 */
@Component("terminateProductDelegate")
public class TerminateProductDelegate extends AbstractEocDelegate {

    private final ServiceRegistryService serviceRegistryService;

    @Autowired
    public TerminateProductDelegate(ServiceRegistryService serviceRegistryService) {
        this.serviceRegistryService = serviceRegistryService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            // Step 1: Resolve the product ID (direct or fallback search)
            String productId = resolveProductId(execution);

            // Step 2: Patch product status to "terminated"
            serviceRegistryService.patchProduct(productId, Map.of("status", "terminated"));

            // Step 3: Read the registered service ID and patch service status to "inactive"
            String serviceId = getRequiredString(execution, "registeredServiceId");
            serviceRegistryService.patchService(serviceId, Map.of("status", "inactive"));
        } catch (BpmnError e) {
            // Re-throw BpmnErrors as-is (ERR_MISSING_VARIABLE)
            throw e;
        } catch (Exception e) {
            throw new BpmnError("ERR_PRODUCT_TERMINATION_FAILED",
                "Failed to terminate product in Service Registry: " + e.getMessage());
        }
    }

    /**
     * Resolves the product ID either from the direct process variable or by
     * falling back to a search using the customer code.
     */
    @SuppressWarnings("unchecked")
    private String resolveProductId(DelegateExecution execution) {
        // Try to read registeredProductId directly
        Object productIdValue = getOptionalVariable(execution, "registeredProductId");
        if (productIdValue != null) {
            return (String) productIdValue;
        }

        // Fallback: search by customerCode
        String customerCode = getRequiredString(execution, "customerCode");
        Map<String, Object> searchResults = serviceRegistryService.searchProducts(
            Map.of("relatedParty.id", customerCode)
        );

        // Extract product ID from search results
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResults.get("results");
        if (results == null || results.isEmpty()) {
            throw new BpmnError("ERR_MISSING_VARIABLE",
                "Could not find product for customer");
        }

        return (String) results.get(0).get("id");
    }
}
