package com.aaha.ftth.servicemove.controller;

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
public class ServiceMoveController {

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/service-move")
    public ResponseEntity<Map<String, Object>> startServiceMove(@RequestBody Map<String, Object> request) {
        try {
            long connectionId = ((Number) request.get("connectionId")).longValue();
            long newPincode = ((Number) request.get("newPincode")).longValue();

            // Build process variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("connectionId", createVariable(connectionId, "Long"));
            variables.put("newPincode", createVariable(newPincode, "Long"));

            // Start process instance
            Map<String, Object> startBody = new HashMap<>();
            startBody.put("variables", variables);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(startBody, headers);

            String startUrl = camundaUrl + "/process-definition/key/Process_ServiceMove/start";
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

        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(500);

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> historyResponse = restTemplate.getForObject(historyUrl, Map.class);

                if (historyResponse != null && historyResponse.get("endTime") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> variablesList = restTemplate.getForObject(variablesUrl, List.class);

                    Map<String, Object> result = new HashMap<>();
                    if (variablesList != null) {
                        for (Map<String, Object> var : variablesList) {
                            String name = (String) var.get("name");
                            Object value = var.get("value");
                            if ("success".equals(name) || "message".equals(name) ||
                                    "areaActive".equals(name) || "availablePorts".equals(name)) {
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
