package com.aaha.ftth.disconnect.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class DisconnectWorker {

    private static final Logger log = LoggerFactory.getLogger(DisconnectWorker.class);
    private static final String WORKER_ID = "disconnect-worker";

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public DisconnectWorker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckConnection() {
        pollTopic("dc_checkConnection", this::handleCheckConnection);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollDisconnect() {
        pollTopic("dc_disconnect", this::handleDisconnect);
    }

    // ======================== TOPIC HANDLERS ========================

    private Map<String, Object> handleCheckConnection(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long connectionId = getLongVariable(variables, "connectionId");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT connection_id, connection_status FROM customer_connections WHERE connection_id = ?", connectionId);

            if (!rows.isEmpty()) {
                String status = (String) rows.get(0).get("connection_status");
                result.put("connectionActive", createVariable("ACTIVE".equals(status), "Boolean"));
            } else {
                result.put("connectionActive", createVariable(false, "Boolean"));
            }
        } catch (Exception e) {
            log.error("Error in dc_checkConnection: {}", e.getMessage(), e);
            result.put("connectionActive", createVariable(false, "Boolean"));
        }
        return result;
    }

    private Map<String, Object> handleDisconnect(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long connectionId = getLongVariable(variables, "connectionId");

            // Step 1: Get port_id and customer_id
            Map<String, Object> connInfo = jdbcTemplate.queryForMap(
                    "SELECT port_id, customer_id FROM customer_connections WHERE connection_id = ?", connectionId);
            Long portId = ((Number) connInfo.get("port_id")).longValue();
            Long customerId = ((Number) connInfo.get("customer_id")).longValue();

            // Step 2: Update connection status
            jdbcTemplate.update(
                    "UPDATE customer_connections SET connection_status = 'DISCONNECTED', disconnected_on = CURDATE(), updated_by = 1 WHERE connection_id = ?",
                    connectionId);

            // Step 3: Release port
            jdbcTemplate.update("UPDATE ports SET port_status = 'AVAILABLE' WHERE port_id = ?", portId);

            // Step 4: Set customer inactive
            jdbcTemplate.update("UPDATE customers SET status = 'INACTIVE' WHERE customer_id = ?", customerId);

            // Step 5: Get email
            String email = jdbcTemplate.queryForObject(
                    "SELECT email FROM customers WHERE customer_id = ?", String.class, customerId);

            // Step 6: Log email
            jdbcTemplate.update(
                    "INSERT INTO email_logs (customer_id, email_type, recipient_email, subject, sent_status, provider_response) " +
                            "VALUES (?, 'DISCONNECT', ?, 'FTTH Disconnection Confirmation', 'SENT', 'SMTP_OK')",
                    customerId, email);

            result.put("success", createVariable(true, "Boolean"));
            result.put("message", createVariable("Connection disconnected successfully", "String"));

        } catch (Exception e) {
            log.error("Error in dc_disconnect: {}", e.getMessage(), e);
            result.put("success", createVariable(false, "Boolean"));
            result.put("message", createVariable("Error: " + e.getMessage(), "String"));
        }
        return result;
    }

    // ======================== GENERIC POLL METHOD ========================

    @FunctionalInterface
    private interface TaskHandler {
        Map<String, Object> handle(Map<String, Object> variables);
    }

    private void pollTopic(String topicName, TaskHandler handler) {
        try {
            Map<String, Object> fetchBody = new HashMap<>();
            fetchBody.put("workerId", WORKER_ID);
            fetchBody.put("maxTasks", 1);
            fetchBody.put("topics", List.of(Map.of(
                    "topicName", topicName,
                    "lockDuration", 30000
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> fetchEntity = new HttpEntity<>(fetchBody, headers);

            String fetchUrl = camundaUrl + "/external-task/fetchAndLock";
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = restTemplate.postForObject(fetchUrl, fetchEntity, List.class);

            if (tasks == null || tasks.isEmpty()) {
                return;
            }

            Map<String, Object> task = tasks.get(0);
            String taskId = (String) task.get("id");

            @SuppressWarnings("unchecked")
            Map<String, Object> vars = (Map<String, Object>) task.get("variables");

            log.info("Processing topic '{}', taskId: {}", topicName, taskId);

            Map<String, Object> resultVariables = handler.handle(vars);

            Map<String, Object> completeBody = new HashMap<>();
            completeBody.put("workerId", WORKER_ID);
            completeBody.put("variables", resultVariables);

            HttpEntity<Map<String, Object>> completeEntity = new HttpEntity<>(completeBody, headers);
            String completeUrl = camundaUrl + "/external-task/" + taskId + "/complete";
            restTemplate.postForObject(completeUrl, completeEntity, String.class);

            log.info("Completed topic '{}', taskId: {}", topicName, taskId);

        } catch (Exception e) {
            if (!e.getMessage().contains("Connection refused")) {
                log.debug("Poll topic '{}': {}", topicName, e.getMessage());
            }
        }
    }

    // ======================== UTILITY METHODS ========================

    private Map<String, Object> createVariable(Object value, String type) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("value", value);
        variable.put("type", type);
        return variable;
    }

    private Long getLongVariable(Map<String, Object> variables, String name) {
        if (variables == null || !variables.containsKey(name)) return null;
        Object varObj = variables.get(name);
        if (varObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> varMap = (Map<String, Object>) varObj;
            Object value = varMap.get("value");
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof String) return Long.parseLong((String) value);
            return null;
        }
        if (varObj instanceof Number) return ((Number) varObj).longValue();
        return null;
    }

    private Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        if (value instanceof String) return "1".equals(value) || "true".equalsIgnoreCase((String) value);
        return false;
    }
}
