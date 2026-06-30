package com.aaha.ftth.newconnection.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NewConnectionController {

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/new-connection")
    public ResponseEntity<Map<String, Object>> startNewConnection(@RequestBody Map<String, Object> request) {
        try {
            // Extract variables from request
            String customerName = (String) request.get("customerName");
            String email = (String) request.get("email");
            String panNumber = (String) request.get("panNumber");
            String dob = (String) request.get("dob"); // format: YYYY-MM-DD
            String pincode = String.valueOf(request.get("pincode"));
            long planId = ((Number) request.get("planId")).longValue();
            String oltType = (String) request.get("oltType");

            // Build process variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", createVariable(customerName, "String"));
            variables.put("email", createVariable(email, "String"));
            variables.put("panNumber", createVariable(panNumber, "String"));
            variables.put("dob", createVariable(dob, "String"));
            variables.put("pincode", createVariable(pincode, "String"));
            variables.put("planId", createVariable(planId, "Long"));
            variables.put("oltType", createVariable(oltType, "String"));

            // Start process instance
            Map<String, Object> startBody = new HashMap<>();
            startBody.put("variables", variables);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(startBody, headers);

            String startUrl = camundaUrl + "/process-definition/key/Process_NewConnection/start";
            @SuppressWarnings("unchecked")
            Map<String, Object> startResponse = restTemplate.postForObject(startUrl, entity, Map.class);

            if (startResponse == null || !startResponse.containsKey("id")) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "success", false,
                        "message", "Failed to start process instance"
                ));
            }

            String processInstanceId = (String) startResponse.get("id");

            // Poll history API for process completion (up to 15 seconds)
            Map<String, Object> result = pollForCompletion(processInstanceId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    private Map<String, Object> pollForCompletion(String processInstanceId) throws InterruptedException {
        String historyUrl = camundaUrl + "/history/process-instance/" + processInstanceId;
        String variablesUrl = camundaUrl + "/history/variable-instance?processInstanceId=" + processInstanceId;

        int maxAttempts = 30; // 15 seconds with 500ms intervals
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(500);

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> historyResponse = restTemplate.getForObject(historyUrl, Map.class);

                if (historyResponse != null && historyResponse.get("endTime") != null) {
                    // Process completed, get result variables
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> variablesList = restTemplate.getForObject(variablesUrl, List.class);

                    Map<String, Object> result = new HashMap<>();
                    if (variablesList != null) {
                        for (Map<String, Object> var : variablesList) {
                            String name = (String) var.get("name");
                            Object value = var.get("value");
                            if ("success".equals(name) || "connectionId".equals(name) || "message".equals(name)) {
                                result.put(name, value);
                            }
                        }
                    }

                    if (result.isEmpty()) {
                        result.put("success", false);
                        result.put("message", "Process completed but no result variables found");
                    }

                    return result;
                }
            } catch (Exception e) {
                // Process not yet complete, continue polling
            }
        }

        // Timeout
        Map<String, Object> timeout = new HashMap<>();
        timeout.put("success", false);
        timeout.put("message", "Process did not complete within 15 seconds");
        timeout.put("processInstanceId", processInstanceId);
        return timeout;
    }

    private Map<String, Object> createVariable(Object value, String type) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("value", value);
        variable.put("type", type);
        return variable;
    }
}
