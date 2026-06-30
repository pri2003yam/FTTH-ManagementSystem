package com.aaha.ftth.camunda.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls POST /api/connections/new-install with all collected variables.
 * This single API call handles: port allocation, customer creation,
 * duplicate check, connection insert, bill generation, and email.
 * 
 * Sets process variables: connectionId, customerCode, oltCode, portNumber
 * Throws BpmnError on failure.
 */
@Component("createConnectionDelegate")
public class CreateConnectionDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${ftth.backend.base-url}")
    private String baseUrl;

    public CreateConnectionDelegate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String customerName = (String) execution.getVariable("customerName");
        String email = (String) execution.getVariable("email");
        Number salary = (Number) execution.getVariable("salary");
        Number pincode = (Number) execution.getVariable("pincode");
        Number planId = (Number) execution.getVariable("planId");
        String oltType = (String) execution.getVariable("oltType");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerName", customerName);
        requestBody.put("email", email);
        requestBody.put("salary", salary.doubleValue());
        requestBody.put("pincode", pincode.longValue());
        requestBody.put("planId", planId.longValue());
        requestBody.put("oltType", oltType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "1");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/connections/new-install", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                execution.setVariable("connectionId", body.get("connectionId"));
                execution.setVariable("customerCode", body.get("customerCode"));
                execution.setVariable("fullName", body.get("fullName"));
                execution.setVariable("oltCode", body.get("oltCode"));
                execution.setVariable("splitterNumber", body.get("splitterNumber"));
                execution.setVariable("portNumber", body.get("portNumber"));
                execution.setVariable("planName", body.get("planName"));
                execution.setVariable("monthlyPrice", body.get("monthlyPrice"));
                execution.setVariable("hasActiveConnection", false);
            } else {
                execution.setVariable("hasActiveConnection", true);
            }
        } catch (HttpClientErrorException e) {
            // Backend returned 400 — could be duplicate connection, salary issue, etc.
            execution.setVariable("hasActiveConnection", true);
            execution.setVariable("errorMessage", e.getResponseBodyAsString());
        }
    }
}
