package com.aaha.ftth.planchange.worker;

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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class PlanChangeWorker {

    private static final Logger log = LoggerFactory.getLogger(PlanChangeWorker.class);
    private static final String WORKER_ID = "plan-change-worker";

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public PlanChangeWorker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollValidatePlan() {
        pollTopic("pc_validatePlan", this::handleValidatePlan);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckConnection() {
        pollTopic("pc_checkConnection", this::handleCheckConnection);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollChangePlan() {
        pollTopic("pc_changePlan", this::handleChangePlan);
    }

    // ======================== TOPIC HANDLERS ========================

    private Map<String, Object> handleValidatePlan(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long newPlanId = getLongVariable(variables, "newPlanId");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT plan_id, is_active, olt_type FROM plans WHERE plan_id = ?", newPlanId);

            if (!rows.isEmpty()) {
                Map<String, Object> plan = rows.get(0);
                Boolean isActive = parseBoolean(plan.get("is_active"));
                result.put("planValid", createVariable(isActive, "Boolean"));
            } else {
                result.put("planValid", createVariable(false, "Boolean"));
            }
        } catch (Exception e) {
            log.error("Error in pc_validatePlan: {}", e.getMessage(), e);
            result.put("planValid", createVariable(false, "Boolean"));
        }
        return result;
    }

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
            log.error("Error in pc_checkConnection: {}", e.getMessage(), e);
            result.put("connectionActive", createVariable(false, "Boolean"));
        }
        return result;
    }

    private Map<String, Object> handleChangePlan(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long connectionId = getLongVariable(variables, "connectionId");
            Long newPlanId = getLongVariable(variables, "newPlanId");

            // Step 1: Get old plan
            Long oldPlanId = jdbcTemplate.queryForObject(
                    "SELECT plan_id FROM customer_connections WHERE connection_id = ?",
                    Long.class, connectionId);

            // Step 2: Get old plan OLT type
            String oldOltType = jdbcTemplate.queryForObject(
                    "SELECT olt_type FROM plans WHERE plan_id = ?",
                    String.class, oldPlanId);

            // Step 3: Get new plan OLT type and monthly price
            Map<String, Object> newPlan = jdbcTemplate.queryForMap(
                    "SELECT olt_type, monthly_price FROM plans WHERE plan_id = ?", newPlanId);
            String newOltType = (String) newPlan.get("olt_type");
            Double newMonthlyPrice = ((Number) newPlan.get("monthly_price")).doubleValue();

            // Step 4: If OLT types differ, reallocate port
            if (!oldOltType.equals(newOltType)) {
                Map<String, Object> connInfo = jdbcTemplate.queryForMap(
                        "SELECT service_area_id, port_id FROM customer_connections WHERE connection_id = ?", connectionId);
                Long serviceAreaId = ((Number) connInfo.get("service_area_id")).longValue();
                Long oldPortId = ((Number) connInfo.get("port_id")).longValue();

                // Allocate new port
                List<Map<String, Object>> availablePorts = jdbcTemplate.queryForList(
                        "SELECT p.port_id FROM ports p " +
                                "JOIN splitters s ON s.splitter_id = p.splitter_id " +
                                "JOIN olts o ON o.olt_id = s.olt_id " +
                                "WHERE o.service_area_id = ? AND o.olt_type = ? AND p.port_status = 'AVAILABLE' LIMIT 1",
                        serviceAreaId, newOltType);

                if (availablePorts.isEmpty()) {
                    result.put("success", createVariable(false, "Boolean"));
                    result.put("message", createVariable("No available ports for new OLT type", "String"));
                    return result;
                }

                Long newPortId = ((Number) availablePorts.get(0).get("port_id")).longValue();

                // Assign new port
                jdbcTemplate.update("UPDATE ports SET port_status = 'ASSIGNED' WHERE port_id = ?", newPortId);
                // Release old port
                jdbcTemplate.update("UPDATE ports SET port_status = 'AVAILABLE' WHERE port_id = ?", oldPortId);
                // Update connection port
                jdbcTemplate.update("UPDATE customer_connections SET port_id = ? WHERE connection_id = ?", newPortId, connectionId);
            }

            // Step 5: Update plan on connection
            jdbcTemplate.update(
                    "UPDATE customer_connections SET plan_id = ?, updated_by = 1 WHERE connection_id = ?",
                    newPlanId, connectionId);

            // Step 6: Generate differential bill (pro-rata)
            Double oldMonthlyPrice = jdbcTemplate.queryForObject(
                    "SELECT monthly_price FROM plans WHERE plan_id = ?", Double.class, oldPlanId);

            LocalDate today = LocalDate.now();
            LocalDate nextBillingDate;
            if (today.getDayOfMonth() <= 10) {
                nextBillingDate = today.withDayOfMonth(10);
            } else {
                nextBillingDate = today.plusMonths(1).withDayOfMonth(10);
            }

            long daysUntilBilling = ChronoUnit.DAYS.between(today, nextBillingDate);
            double dailyDiff = (newMonthlyPrice - oldMonthlyPrice) / 30.0;
            double planCharge = Math.round(dailyDiff * daysUntilBilling * 100.0) / 100.0;
            double gstAmount = Math.round(planCharge * 0.18 * 100.0) / 100.0;
            double totalAmount = Math.round((planCharge + gstAmount) * 100.0) / 100.0;

            Long customerId = jdbcTemplate.queryForObject(
                    "SELECT customer_id FROM customer_connections WHERE connection_id = ?",
                    Long.class, connectionId);

            String billNo = "BILL-PC-" + System.currentTimeMillis();
            LocalDate dueDate = today.plusDays(15);

            jdbcTemplate.update(
                    "INSERT INTO bills (bill_no, customer_id, connection_id, bill_date, due_date, plan_charge, gst_amount, total_amount, bill_status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'GENERATED')",
                    billNo, customerId, connectionId, today, dueDate, planCharge, gstAmount, totalAmount);

            // Step 7: Log email
            String email = jdbcTemplate.queryForObject(
                    "SELECT email FROM customers WHERE customer_id = ?", String.class, customerId);

            jdbcTemplate.update(
                    "INSERT INTO email_logs (customer_id, email_type, recipient_email, subject, sent_status, provider_response) " +
                            "VALUES (?, 'PLAN_CHANGE', ?, 'FTTH Plan Change Confirmation', 'SENT', 'SMTP_OK')",
                    customerId, email);

            result.put("success", createVariable(true, "Boolean"));
            result.put("message", createVariable("Plan changed successfully", "String"));

        } catch (Exception e) {
            log.error("Error in pc_changePlan: {}", e.getMessage(), e);
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
            Map<String, Object> variables = (Map<String, Object>) task.get("variables");

            log.info("Processing topic '{}', taskId: {}", topicName, taskId);

            Map<String, Object> resultVariables = handler.handle(variables);

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
