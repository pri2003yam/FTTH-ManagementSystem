package com.aaha.ftth.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls GET /api/plans/{planId} to validate the plan exists and is active.
 * Sets process variable: planValid (boolean)
 */
@Component("findPlanDelegate")
public class FindPlanDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${ftth.backend.base-url}")
    private String baseUrl;

    public FindPlanDelegate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long planId = ((Number) execution.getVariable("planId")).longValue();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/api/plans/" + planId, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean isActive = (Boolean) response.getBody().get("active");
                execution.setVariable("planValid", isActive != null && isActive);
                execution.setVariable("planName", response.getBody().get("planName"));
                execution.setVariable("oltType", response.getBody().get("oltType"));
            } else {
                execution.setVariable("planValid", false);
            }
        } catch (Exception e) {
            execution.setVariable("planValid", false);
        }
    }
}
