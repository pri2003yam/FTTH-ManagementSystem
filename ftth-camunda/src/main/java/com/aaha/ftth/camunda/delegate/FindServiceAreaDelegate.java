package com.aaha.ftth.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Checks if the pincode has inventory (service area exists & active).
 * Calls GET /api/inventory/pincodes and checks if the given pincode is in the list.
 * Sets process variable: areaActive (boolean)
 */
@Component("findServiceAreaDelegate")
public class FindServiceAreaDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${ftth.backend.base-url}")
    private String baseUrl;

    public FindServiceAreaDelegate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String pincode = String.valueOf(execution.getVariable("pincode"));

        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/api/inventory/pincodes", List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                boolean found = response.getBody().contains(pincode);
                execution.setVariable("areaActive", found);
            } else {
                execution.setVariable("areaActive", false);
            }
        } catch (Exception e) {
            execution.setVariable("areaActive", false);
        }
    }
}
