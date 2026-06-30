package com.aaha.ftth.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Checks port availability for the given pincode and OLT type.
 * Calls GET /api/capacity and filters by pincode + oltType.
 * Sets process variable: availablePorts (int)
 */
@Component("checkPortsDelegate")
public class CheckPortsDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${ftth.backend.base-url}")
    private String baseUrl;

    public CheckPortsDelegate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String pincode = String.valueOf(execution.getVariable("pincode"));
        String oltType = (String) execution.getVariable("oltType");

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/api/capacity", Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> olts = (List<Map<String, Object>>) response.getBody().get("olts");
                int freePorts = 0;

                if (olts != null) {
                    for (Map<String, Object> olt : olts) {
                        String oltPincode = String.valueOf(olt.get("pincode"));
                        String oltOltType = String.valueOf(olt.get("oltType"));

                        if (oltPincode.equals(pincode) && oltOltType.equals(oltType)) {
                            freePorts += ((Number) olt.get("freePorts")).intValue();
                        }
                    }
                }

                execution.setVariable("availablePorts", freePorts);
            } else {
                execution.setVariable("availablePorts", 0);
            }
        } catch (Exception e) {
            execution.setVariable("availablePorts", 0);
        }
    }
}
