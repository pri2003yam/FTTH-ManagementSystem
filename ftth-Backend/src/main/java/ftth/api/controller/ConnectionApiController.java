package ftth.api.controller;

import ftth.model.CustomerConnection;
import ftth.model.Plan;
import ftth.model.dtos.AddConnectionRequest;
import ftth.repository.*;
import ftth.service.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/connections")
public class ConnectionApiController {

    private final CustomerConnectionService customerConnectionService;
    private final PlanService planService;
    private final InventoryService inventoryService;
    private final CustomerConnectionRepository connectionRepo;

    public ConnectionApiController() {
        CustomerRepository customerRepo = new CustomerRepository();
        CustomerConnectionRepository connRepo = new CustomerConnectionRepository();
        BillRepository billRepo = new BillRepository();
        ServiceAreaRepository serviceAreaRepo = new ServiceAreaRepository();
        PlanRepository planRepo = new PlanRepository();
        InventoryRepository inventoryRepo = new InventoryRepository();
        OltRepository oltRepo = new OltRepository();
        EmailLogRepository emailLogRepo = new EmailLogRepository();

        EmailService emailService = new EmailService(emailLogRepo);
        CustomerService customerService = new CustomerService(customerRepo, planRepo, oltRepo);
        PlanService ps = new PlanService(planRepo, emailService);
        InventoryService is = new InventoryService(inventoryRepo);
        ServiceAreaService serviceAreaService = new ServiceAreaService(serviceAreaRepo);

        this.customerConnectionService = new CustomerConnectionService(
            customerService, customerRepo, connRepo,
            ps, is, billRepo, serviceAreaService, emailService
        );
        this.planService = ps;
        this.inventoryService = is;
        this.connectionRepo = connRepo;
    }

    // ── New Install ──────────────────────────────────────────────
    @PostMapping("/new-install")
    public ResponseEntity<Map<String, Object>> newInstall(
            @RequestBody AddConnectionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        Map<String, Object> result = new HashMap<>();
        try {
            CustomerConnection conn = customerConnectionService.createConnection(req, userId);
            result.put("message", "Connection created successfully.");
            if (conn != null) {
                // fetch port/splitter/olt details
                String sql =
                    "SELECT cc.connection_id, c.customer_code, c.full_name, sa.pincode, " +
                    "p.plan_name, p.monthly_price, o.olt_type, o.olt_code, " +
                    "s.splitter_number, pt.port_number " +
                    "FROM customer_connections cc " +
                    "JOIN customers c ON c.customer_id = cc.customer_id " +
                    "JOIN plans p ON p.plan_id = cc.plan_id " +
                    "JOIN ports pt ON pt.port_id = cc.port_id " +
                    "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
                    "JOIN olts o ON o.olt_id = s.olt_id " +
                    "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
                    "WHERE cc.connection_id = ?";
                try (java.sql.Connection con = ftth.config.DbConnection.getConnection();
                     java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, conn.getConnectionId());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            result.put("connectionId",   rs.getLong("connection_id"));
                            result.put("customerCode",   rs.getString("customer_code"));
                            result.put("fullName",       rs.getString("full_name"));
                            result.put("pincode",        rs.getString("pincode"));
                            result.put("planName",       rs.getString("plan_name"));
                            result.put("monthlyPrice",   rs.getDouble("monthly_price"));
                            result.put("oltType",        rs.getString("olt_type"));
                            result.put("oltCode",        rs.getString("olt_code"));
                            result.put("splitterNumber", rs.getInt("splitter_number"));
                            result.put("portNumber",     rs.getInt("port_number"));
                        }
                    }
                }
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to create connection.");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ── List Active Connections ──────────────────────────────────
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveConnections() {
        String sql =
            "SELECT cc.connection_id, c.customer_code, c.full_name, sa.pincode, " +
            "p.plan_name, p.monthly_price, o.olt_type, o.olt_code, " +
            "s.splitter_number, pt.port_number, cc.plan_id, cc.service_area_id " +
            "FROM customer_connections cc " +
            "JOIN customers c ON c.customer_id = cc.customer_id " +
            "JOIN plans p ON p.plan_id = cc.plan_id " +
            "JOIN ports pt ON pt.port_id = cc.port_id " +
            "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
            "JOIN olts o ON o.olt_id = s.olt_id " +
            "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
            "WHERE cc.connection_status = 'ACTIVE' " +
            "ORDER BY cc.connection_id";

        List<Map<String, Object>> rows = new ArrayList<>();
        try (java.sql.Connection con = ftth.config.DbConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("connectionId",    rs.getLong("connection_id"));
                row.put("customerCode",    rs.getString("customer_code"));
                row.put("fullName",        rs.getString("full_name"));
                row.put("pincode",         rs.getString("pincode"));
                row.put("planName",        rs.getString("plan_name"));
                row.put("monthlyPrice",    rs.getDouble("monthly_price"));
                row.put("oltType",         rs.getString("olt_type"));
                row.put("oltCode",         rs.getString("olt_code"));
                row.put("splitterNumber",  rs.getInt("splitter_number"));
                row.put("portNumber",      rs.getInt("port_number"));
                row.put("planId",          rs.getLong("plan_id"));
                row.put("serviceAreaId",   rs.getLong("service_area_id"));
                rows.add(row);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(rows);
    }

    // ── Available Plans for a Connection ─────────────────────────
    @GetMapping("/{connectionId}/available-plans")
    public ResponseEntity<List<Map<String, Object>>> getAvailablePlans(
            @PathVariable(value = "connectionId") Long connectionId) {

        try {
            CustomerConnection conn = connectionRepo.findById(connectionId);
            if (conn == null || !conn.isActive()) {
                return ResponseEntity.notFound().build();
            }

            Plan currentPlan = planService.findPlanById(conn.getPlanId());
            if (currentPlan == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Plan> allPlans = new ArrayList<>(planService.getActivePlans());
            List<Map<String, Object>> result = new ArrayList<>();

            for (Plan p : allPlans) {
                if (p.getPlanId().equals(currentPlan.getPlanId())) continue;
                int ports = inventoryService.getAvailablePortsByType(conn.getServiceAreaId(), p.getOltType());
                if (ports <= 0) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("planId",         p.getPlanId());
                m.put("planName",       p.getPlanName());
                m.put("speedLabel",     p.getSpeedLabel());
                m.put("dataLimitLabel", p.getDataLimitLabel());
                m.put("ottCount",       p.getOttCount());
                m.put("monthlyPrice",   p.getMonthlyPrice());
                m.put("oltType",        p.getOltType());
                result.add(m);
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(List.of(err));
        }
    }

    // ── Change Plan ──────────────────────────────────────────────
    @PostMapping("/{connectionId}/change-plan")
    public ResponseEntity<Map<String, String>> changePlan(
            @PathVariable(value = "connectionId") Long connectionId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        Map<String, String> result = new HashMap<>();
        try {
            long newPlanId = Long.parseLong(body.get("planId").toString());
            customerConnectionService.changePlan(connectionId, newPlanId, userId);
            result.put("message", "Plan changed successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to change plan.");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ── Check Move Availability ──────────────────────────────────
    @GetMapping("/{connectionId}/check-move")
    public ResponseEntity<Map<String, Object>> checkMove(
            @PathVariable(value = "connectionId") Long connectionId,
            @RequestParam(value = "pincode") String pincode) {

        Map<String, Object> result = new HashMap<>();
        try {
            CustomerConnection conn = connectionRepo.findById(connectionId);
            if (conn == null || !conn.isActive()) {
                result.put("available", false);
                result.put("message", "Connection not found or inactive.");
                return ResponseEntity.ok(result);
            }

            Plan plan = planService.findPlanById(conn.getPlanId());
            String oltType = plan.getOltType();

            ftth.model.ServiceArea newArea = new ftth.service.ServiceAreaService(
                new ftth.repository.ServiceAreaRepository()
            ).findByPincode(Long.parseLong(pincode));

            if (newArea == null || !newArea.isActive()) {
                result.put("available", false);
                result.put("oltType", oltType);
                result.put("availablePorts", 0);
                result.put("message", "Service not available in pincode " + pincode + ".");
                return ResponseEntity.ok(result);
            }

            int ports = inventoryService.getAvailablePortsByType(newArea.getServiceAreaId(), oltType);
            result.put("oltType", oltType);
            result.put("availablePorts", ports);
            result.put("available", ports > 0);
            result.put("message", ports > 0
                ? ports + " " + oltType + " port(s) available in pincode " + pincode + "."
                : "No " + oltType + " ports available in pincode " + pincode + ".");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("available", false);
            result.put("message", e.getMessage() != null ? e.getMessage() : "Error checking move.");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ── Move Connection ──────────────────────────────────────────
    @PostMapping("/{connectionId}/move")
    public ResponseEntity<Map<String, String>> moveConnection(
            @PathVariable(value = "connectionId") Long connectionId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        Map<String, String> result = new HashMap<>();
        try {
            long newPincode = Long.parseLong(body.get("newPincode").toString());

            CustomerConnection conn = connectionRepo.findById(connectionId);
            if (conn == null || !conn.isActive()) {
                result.put("message", "Connection not found or inactive.");
                return ResponseEntity.badRequest().body(result);
            }

            Plan plan = planService.findPlanById(conn.getPlanId());
            String oltType = plan.getOltType();

            customerConnectionService.updateCustomerConnection(conn, newPincode, oltType, userId);
            result.put("message", "Customer moved successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to move connection.");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ── Disconnect ───────────────────────────────────────────────
    @PostMapping("/{connectionId}/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(
            @PathVariable(value = "connectionId") Long connectionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        Map<String, String> result = new HashMap<>();
        try {
            customerConnectionService.disconnect(connectionId, userId);
            result.put("message", "Connection disconnected successfully.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to disconnect.");
            return ResponseEntity.badRequest().body(result);
        }
    }
}
