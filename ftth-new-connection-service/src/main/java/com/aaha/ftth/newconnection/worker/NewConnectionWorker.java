package com.aaha.ftth.newconnection.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class NewConnectionWorker {

    private static final Logger log = LoggerFactory.getLogger(NewConnectionWorker.class);
    private static final String WORKER_ID = "new-connection-worker";

    @Value("${camunda.rest.url}")
    private String camundaUrl;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public NewConnectionWorker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollKycVerify() {
        pollTopic("kycVerify", this::handleKycVerify);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollValidatePlan() {
        pollTopic("validatePlan", this::handleValidatePlan);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckServiceArea() {
        pollTopic("checkServiceArea", this::handleCheckServiceArea);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCheckPorts() {
        pollTopic("checkPorts", this::handleCheckPorts);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollCreateConnection() {
        pollTopic("createConnection", this::handleCreateConnection);
    }

    // ======================== TOPIC HANDLERS ========================

    /**
     * KYC VERIFICATION
     * Reads: panNumber, customerName, dob
     * Checks the kyc_records table for PAN validation
     * Sets: kycPassed (Boolean), kycReason (String)
     * 
     * Error cases:
     * - PAN not found
     * - PAN inactive / surrendered / deceased
     * - Name mismatch with PAN
     * - Date of Birth mismatch
     */
    private Map<String, Object> handleKycVerify(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            String panNumber = getStringVariable(variables, "panNumber");
            String customerName = getStringVariable(variables, "customerName");
            String dob = getStringVariable(variables, "dob"); // format: YYYY-MM-DD

            log.info("🔍 [KYC] Verifying PAN: {}, Name: {}, DOB: {}", panNumber, customerName, dob);

            // Step 1: Look up PAN in kyc_records
            List<Map<String, Object>> records = jdbcTemplate.queryForList(
                    "SELECT pan_number, full_name, date_of_birth, pan_status FROM kyc_records WHERE pan_number = ?",
                    panNumber);

            if (records.isEmpty()) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("PAN not found", "String"));
                log.info("❌ [KYC] FAILED: PAN not found - {}", panNumber);
                return result;
            }

            Map<String, Object> record = records.get(0);
            String panStatus = (String) record.get("pan_status");
            String registeredName = (String) record.get("full_name");
            String registeredDob = record.get("date_of_birth").toString(); // comes as java.sql.Date

            // Step 2: Check PAN status
            if ("INACTIVE".equals(panStatus)) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("PAN inactive", "String"));
                log.info("❌ [KYC] FAILED: PAN inactive - {}", panNumber);
                return result;
            }
            if ("SURRENDERED".equals(panStatus)) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("PAN surrendered", "String"));
                log.info("❌ [KYC] FAILED: PAN surrendered - {}", panNumber);
                return result;
            }
            if ("DECEASED".equals(panStatus)) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("PAN linked to deceased person", "String"));
                log.info("❌ [KYC] FAILED: PAN linked to deceased - {}", panNumber);
                return result;
            }

            // Step 3: Verify name matches (case-insensitive)
            if (!registeredName.equalsIgnoreCase(customerName.trim())) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("Name mismatch with PAN", "String"));
                log.info("❌ [KYC] FAILED: Name mismatch - expected '{}', got '{}'", registeredName, customerName);
                return result;
            }

            // Step 4: Verify DOB matches
            if (!registeredDob.equals(dob)) {
                result.put("kycPassed", createVariable(false, "Boolean"));
                result.put("kycReason", createVariable("Date of Birth mismatch", "String"));
                log.info("❌ [KYC] FAILED: DOB mismatch - expected '{}', got '{}'", registeredDob, dob);
                return result;
            }

            // All checks passed!
            result.put("kycPassed", createVariable(true, "Boolean"));
            result.put("kycReason", createVariable("KYC verified successfully", "String"));
            log.info("✅ [KYC] PASSED: PAN={}, Name={}, DOB={}", panNumber, customerName, dob);

        } catch (Exception e) {
            log.error("Error in kycVerify: {}", e.getMessage(), e);
            result.put("kycPassed", createVariable(false, "Boolean"));
            result.put("kycReason", createVariable("KYC verification system error: " + e.getMessage(), "String"));
        }
        return result;
    }

    private Map<String, Object> handleValidatePlan(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long planId = getLongVariable(variables, "planId");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT plan_id, plan_name, olt_type, is_active FROM plans WHERE plan_id = ?", planId);

            if (!rows.isEmpty()) {
                Map<String, Object> plan = rows.get(0);
                Boolean isActive = parseBoolean(plan.get("is_active"));
                String oltType = (String) plan.get("olt_type");

                result.put("planValid", createVariable(isActive, "Boolean"));
                result.put("oltType", createVariable(oltType, "String"));
            } else {
                result.put("planValid", createVariable(false, "Boolean"));
            }
        } catch (Exception e) {
            log.error("Error in validatePlan: {}", e.getMessage(), e);
            result.put("planValid", createVariable(false, "Boolean"));
        }
        return result;
    }

    private Map<String, Object> handleCheckServiceArea(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            String pincode = getStringVariable(variables, "pincode");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT service_area_id, is_active FROM service_areas WHERE pincode = ?", pincode);

            if (!rows.isEmpty()) {
                Map<String, Object> area = rows.get(0);
                Boolean isActive = parseBoolean(area.get("is_active"));
                result.put("areaActive", createVariable(isActive, "Boolean"));
            } else {
                result.put("areaActive", createVariable(false, "Boolean"));
            }
        } catch (Exception e) {
            log.error("Error in checkServiceArea: {}", e.getMessage(), e);
            result.put("areaActive", createVariable(false, "Boolean"));
        }
        return result;
    }

    private Map<String, Object> handleCheckPorts(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            String pincode = getStringVariable(variables, "pincode");
            String oltType = getStringVariable(variables, "oltType");

            // Call Reservation Service to reserve a port (BookMyShow pattern)
            Map<String, String> reserveRequest = new HashMap<>();
            reserveRequest.put("pincode", pincode);
            reserveRequest.put("oltType", oltType);

            log.info("Calling Reservation Service: POST http://localhost:8090/api/reserve for pincode={}, oltType={}", pincode, oltType);

            @SuppressWarnings("unchecked")
            Map<String, Object> reserveResponse = restTemplate.postForObject(
                "http://localhost:8090/api/reserve", reserveRequest, Map.class);

            if (reserveResponse != null && "RESERVED".equals(reserveResponse.get("status"))) {
                int portCount = 1; // reserved successfully means at least 1 available
                result.put("availablePorts", createVariable(portCount, "Integer"));
                result.put("reservationId", createVariable(((Number) reserveResponse.get("reservationId")).longValue(), "Long"));
                result.put("reservedPortId", createVariable(((Number) reserveResponse.get("portId")).longValue(), "Long"));
                log.info("✅ Port RESERVED: reservationId={}, portId={}, expiresAt={}", 
                    reserveResponse.get("reservationId"), reserveResponse.get("portId"), reserveResponse.get("expiresAt"));
            } else {
                String reason = reserveResponse != null ? (String) reserveResponse.get("reason") : "Reservation service unavailable";
                result.put("availablePorts", createVariable(0, "Integer"));
                log.info("❌ Reservation FAILED: {}", reason);
            }
        } catch (Exception e) {
            log.error("Error in checkPorts (reservation): {}", e.getMessage(), e);
            result.put("availablePorts", createVariable(0, "Integer"));
        }
        return result;
    }

    private Map<String, Object> handleCreateConnection(Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        try {
            String customerName = getStringVariable(variables, "customerName");
            String email = getStringVariable(variables, "email");
            Long salary = getLongVariable(variables, "salary");
            String pincode = getStringVariable(variables, "pincode");
            Long planId = getLongVariable(variables, "planId");
            String oltType = getStringVariable(variables, "oltType");

            // Step 1: Find or create customer
            Long customerId;
            List<Map<String, Object>> existingCustomers = jdbcTemplate.queryForList(
                    "SELECT customer_id, customer_code FROM customers WHERE full_name = ? AND email = ?",
                    customerName, email);

            if (!existingCustomers.isEmpty()) {
                customerId = ((Number) existingCustomers.get(0).get("customer_id")).longValue();
            } else {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO customers (customer_code, full_name, email, salary, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, "TEMP");
                    ps.setString(2, customerName);
                    ps.setString(3, email);
                    ps.setLong(4, salary);
                    return ps;
                }, keyHolder);

                customerId = keyHolder.getKey().longValue();
                String customerCode = "CUST-" + customerId;
                jdbcTemplate.update("UPDATE customers SET customer_code = ? WHERE customer_id = ?",
                        customerCode, customerId);
            }

            // Step 2: Confirm port reservation (BookMyShow pattern)
            Long reservationId = getLongVariable(variables, "reservationId");
            Long portId = getLongVariable(variables, "reservedPortId");

            if (reservationId != null) {
                // Confirm the reservation via Reservation Service
                log.info("Confirming reservation: POST http://localhost:8090/api/reserve/{}/confirm", reservationId);
                try {
                    restTemplate.postForObject(
                        "http://localhost:8090/api/reserve/" + reservationId + "/confirm", null, Map.class);
                    log.info("✅ Reservation {} CONFIRMED, port {} now ASSIGNED", reservationId, portId);
                } catch (Exception ex) {
                    log.error("Failed to confirm reservation {}: {}", reservationId, ex.getMessage());
                    result.put("success", createVariable(false, "Boolean"));
                    result.put("message", createVariable("Port reservation expired. Please try again.", "String"));
                    return result;
                }
            } else {
                // Fallback: direct allocation if no reservation (shouldn't happen normally)
                List<Map<String, Object>> availablePorts = jdbcTemplate.queryForList(
                        "SELECT p.port_id FROM ports p " +
                                "JOIN splitters s ON s.splitter_id = p.splitter_id " +
                                "JOIN olts o ON o.olt_id = s.olt_id " +
                                "JOIN service_areas sa ON sa.service_area_id = o.service_area_id " +
                                "WHERE sa.pincode = ? AND o.olt_type = ? AND p.port_status = 'AVAILABLE' LIMIT 1",
                        pincode, oltType);

                if (availablePorts.isEmpty()) {
                    result.put("success", createVariable(false, "Boolean"));
                    result.put("message", createVariable("No available ports found", "String"));
                    return result;
                }

                portId = ((Number) availablePorts.get(0).get("port_id")).longValue();
                jdbcTemplate.update("UPDATE ports SET port_status = 'ASSIGNED' WHERE port_id = ?", portId);
            }

            // Step 3: Get service_area_id
            Long serviceAreaId = jdbcTemplate.queryForObject(
                    "SELECT service_area_id FROM service_areas WHERE pincode = ?",
                    Long.class, pincode);

            // Step 4: Insert connection
            final Long finalPortId = portId;
            KeyHolder connKeyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day, created_by) " +
                                "VALUES (?, ?, ?, ?, 'ACTIVE', CURDATE(), 10, 1)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, customerId);
                ps.setLong(2, planId);
                ps.setLong(3, finalPortId);
                ps.setLong(4, serviceAreaId);
                return ps;
            }, connKeyHolder);

            Long connectionId = connKeyHolder.getKey().longValue();

            // Step 5: Generate first bill (pro-rata)
            Double monthlyPrice = jdbcTemplate.queryForObject(
                    "SELECT monthly_price FROM plans WHERE plan_id = ?", Double.class, planId);

            LocalDate today = LocalDate.now();
            LocalDate nextBillingDate;
            if (today.getDayOfMonth() <= 10) {
                nextBillingDate = today.withDayOfMonth(10);
            } else {
                nextBillingDate = today.plusMonths(1).withDayOfMonth(10);
            }

            long daysUntilBilling = ChronoUnit.DAYS.between(today, nextBillingDate);
            double dailyRate = monthlyPrice / 30.0;
            double planCharge = Math.round(dailyRate * daysUntilBilling * 100.0) / 100.0;
            double gstAmount = Math.round(planCharge * 0.18 * 100.0) / 100.0;
            double totalAmount = Math.round((planCharge + gstAmount) * 100.0) / 100.0;

            String billNo = "BILL-" + System.currentTimeMillis();
            LocalDate dueDate = today.plusDays(15);

            jdbcTemplate.update(
                    "INSERT INTO bills (bill_no, customer_id, connection_id, bill_date, due_date, plan_charge, gst_amount, total_amount, bill_status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'GENERATED')",
                    billNo, customerId, connectionId, today, dueDate, planCharge, gstAmount, totalAmount);

            // Step 6: Log email
            jdbcTemplate.update(
                    "INSERT INTO email_logs (customer_id, email_type, recipient_email, subject, sent_status, provider_response) " +
                            "VALUES (?, 'ORDER_CONFIRMATION', ?, 'FTTH Connection Activated', 'SENT', 'SMTP_OK')",
                    customerId, email);

            result.put("success", createVariable(true, "Boolean"));
            result.put("connectionId", createVariable(connectionId, "Long"));
            result.put("message", createVariable("Connection created successfully via BPMN microservice", "String"));

        } catch (Exception e) {
            log.error("Error in createConnection: {}", e.getMessage(), e);
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
            // Fetch and lock
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

            // Execute handler
            Map<String, Object> resultVariables = handler.handle(variables);

            // Complete task
            Map<String, Object> completeBody = new HashMap<>();
            completeBody.put("workerId", WORKER_ID);
            completeBody.put("variables", resultVariables);

            HttpEntity<Map<String, Object>> completeEntity = new HttpEntity<>(completeBody, headers);
            String completeUrl = camundaUrl + "/external-task/" + taskId + "/complete";
            restTemplate.postForObject(completeUrl, completeEntity, String.class);

            log.info("Completed topic '{}', taskId: {}", topicName, taskId);

        } catch (Exception e) {
            // Silently ignore when no tasks available or Camunda is down
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
