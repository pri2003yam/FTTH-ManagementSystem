package com.aaha.ftth.servicemove.worker;

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
public class ServiceMoveWorker {

    private static final Logger log = LoggerFactory.getLogger(ServiceMoveWorker.class);
    private static final String WORKER_ID = "service-move-worker";

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public ServiceMoveWorker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckArea() {
        pollTopic("mv_checkArea", this::handleCheckArea);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckPorts() {
        pollTopic("mv_checkPorts", this::handleCheckPorts);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollMoveConnection() {
        pollTopic("mv_moveConnection", this::handleMoveConnection);
    }

    // ======================== TOPIC HANDLERS ========================

    private Map<String, Object> handleCheckArea(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long newPincode = getLongVariable(variables, "newPincode");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT service_area_id, is_active FROM service_areas WHERE pincode = ?", newPincode);

            if (!rows.isEmpty()) {
                Boolean isActive = parseBoolean(rows.get(0).get("is_active"));
                result.put("areaActive", createVariable(isActive, "Boolean"));
            } else {
                result.put("areaActive", createVariable(false, "Boolean"));
            }
        } catch (Exception e) {
            log.error("Error in mv_checkArea: {}", e.getMessage(), e);
            result.put("areaActive", createVariable(false, "Boolean"));
        }
        return result;
    }

    private Map<String, Object> handleCheckPorts(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long connectionId = getLongVariable(variables, "connectionId");
            Long newPincode = getLongVariable(variables, "newPincode");

            // Get OLT type from connection's plan
            String oltType = jdbcTemplate.queryForObject(
                    "SELECT p.olt_type FROM customer_connections cc " +
                            "JOIN plans p ON p.plan_id = cc.plan_id " +
                            "WHERE cc.connection_id = ?",
                    String.class, connectionId);

            // Count available ports in new area
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ports pt " +
                            "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
                            "JOIN olts o ON o.olt_id = s.olt_id " +
                            "JOIN service_areas sa ON sa.service_area_id = o.service_area_id " +
                            "WHERE sa.pincode = ? AND o.olt_type = ? AND pt.port_status = 'AVAILABLE'",
                    Integer.class, newPincode, oltType);

            result.put("availablePorts", createVariable(count != null ? count : 0, "Integer"));
            result.put("oltType", createVariable(oltType, "String"));

        } catch (Exception e) {
            log.error("Error in mv_checkPorts: {}", e.getMessage(), e);
            result.put("availablePorts", createVariable(0, "Integer"));
            result.put("oltType", createVariable("", "String"));
        }
        return result;
    }

    private Map<String, Object> handleMoveConnection(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long connectionId = getLongVariable(variables, "connectionId");
            Long newPincode = getLongVariable(variables, "newPincode");
            String oltType = getStringVariable(variables, "oltType");

            // Step 1: Get old port_id
            Long oldPortId = jdbcTemplate.queryForObject(
                    "SELECT port_id FROM customer_connections WHERE connection_id = ?",
                    Long.class, connectionId);

            // Step 2: Allocate new port in new area
            List<Map<String, Object>> availablePorts = jdbcTemplate.queryForList(
                    "SELECT p.port_id FROM ports p " +
                            "JOIN splitters s ON s.splitter_id = p.splitter_id " +
                            "JOIN olts o ON o.olt_id = s.olt_id " +
                            "JOIN service_areas sa ON sa.service_area_id = o.service_area_id " +
                            "WHERE sa.pincode = ? AND o.olt_type = ? AND p.port_status = 'AVAILABLE' LIMIT 1",
                    newPincode, oltType);

            if (availablePorts.isEmpty()) {
                result.put("success", createVariable(false, "Boolean"));
                result.put("message", createVariable("No available ports in new area", "String"));
                return result;
            }

            Long newPortId = ((Number) availablePorts.get(0).get("port_id")).longValue();

            // Step 3: Assign new port
            jdbcTemplate.update("UPDATE ports SET port_status = 'ASSIGNED' WHERE port_id = ?", newPortId);

            // Step 4: Release old port
            jdbcTemplate.update("UPDATE ports SET port_status = 'AVAILABLE' WHERE port_id = ?", oldPortId);

            // Step 5: Get new service_area_id
            Long newServiceAreaId = jdbcTemplate.queryForObject(
                    "SELECT service_area_id FROM service_areas WHERE pincode = ?",
                    Long.class, newPincode);

            // Step 6: Update connection
            jdbcTemplate.update(
                    "UPDATE customer_connections SET service_area_id = ?, port_id = ?, updated_by = 1 WHERE connection_id = ?",
                    newServiceAreaId, newPortId, connectionId);

            // Step 7: Get customer email
            Map<String, Object> customerInfo = jdbcTemplate.queryForMap(
                    "SELECT c.customer_id, c.email FROM customers c " +
                            "JOIN customer_connections cc ON cc.customer_id = c.customer_id " +
                            "WHERE cc.connection_id = ?", connectionId);
            Long customerId = ((Number) customerInfo.get("customer_id")).longValue();
            String email = (String) customerInfo.get("email");

            // Step 8: Log email
            jdbcTemplate.update(
                    "INSERT INTO email_logs (customer_id, email_type, recipient_email, subject, sent_status, provider_response) " +
                            "VALUES (?, 'SERVICE_MOVE', ?, 'FTTH Service Move Confirmation', 'SENT', 'SMTP_OK')",
                    customerId, email);

            result.put("success", createVariable(true, "Boolean"));
            result.put("message", createVariable("Service moved successfully", "String"));

        } catch (Exception e) {
            log.error("Error in mv_moveConnection: {}", e.getMessage(), e);
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

    private String getStringVariable(Map<String, Object> variables, String name) {
        if (variables == null || !variables.containsKey(name)) return null;
        Object varObj = variables.get(name);
        if (varObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> varMap = (Map<String, Object>) varObj;
            return (String) varMap.get("value");
        }
        return varObj != null ? varObj.toString() : null;
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
