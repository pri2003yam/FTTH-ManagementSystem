package ftth.api.controller;

import ftth.config.DbConnection;
import ftth.model.*;
import ftth.repository.*;
import ftth.service.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerApiController {

    private final CustomerService customerService;
    private final CustomerConnectionService connectionService;
    private final BillService billService;
    private final PlanService planService;

    public CustomerApiController() {
        CustomerRepository customerRepo = new CustomerRepository();
        CustomerConnectionRepository connRepo = new CustomerConnectionRepository();
        BillRepository billRepo = new BillRepository();
        ServiceAreaRepository serviceAreaRepo = new ServiceAreaRepository();
        PlanRepository planRepo = new PlanRepository();
        InventoryRepository inventoryRepo = new InventoryRepository();
        OltRepository oltRepo = new OltRepository();
        EmailLogRepository emailLogRepo = new EmailLogRepository();

        EmailService emailService = new EmailService(emailLogRepo);
        this.customerService = new CustomerService(customerRepo, planRepo, oltRepo);
        PlanService ps = new PlanService(planRepo, emailService);
        InventoryService is = new InventoryService(inventoryRepo);
        ServiceAreaService serviceAreaService = new ServiceAreaService(serviceAreaRepo);

        this.connectionService = new CustomerConnectionService(
            customerService, customerRepo, connRepo,
            ps, is, billRepo, serviceAreaService, emailService
        );
        this.planService = ps;
        this.billService = new BillService(billRepo);
    }

    // ── List All Customers ───────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listCustomers() {
        String sql =
            "SELECT c.customer_id, c.customer_code, c.full_name, c.email, c.status, " +
            "sa.pincode, p.plan_name " +
            "FROM customers c " +
            "LEFT JOIN customer_connections cc ON cc.customer_id = c.customer_id " +
            "  AND cc.connection_id = (SELECT MAX(cc2.connection_id) FROM customer_connections cc2 WHERE cc2.customer_id = c.customer_id) " +
            "LEFT JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
            "LEFT JOIN plans p ON p.plan_id = cc.plan_id " +
            "ORDER BY c.customer_id";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("customerId", rs.getLong("customer_id"));
                row.put("customerCode", rs.getString("customer_code"));
                row.put("fullName", rs.getString("full_name"));
                row.put("email", rs.getString("email"));
                row.put("status", rs.getString("status"));
                row.put("pincode", rs.getString("pincode"));
                row.put("planName", rs.getString("plan_name"));
                result.add(row);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(result);
    }

    // ── Get Customer by Code ─────────────────────────────────────
    @GetMapping("/{customerCode}")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @PathVariable(value = "customerCode") String customerCode) {

        Customer c = customerService.lookupCustomerByCode(customerCode);
        if (c == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", c.getCustomerId());
        result.put("customerCode", c.getCustomerCode());
        result.put("fullName", c.getFullName());
        result.put("email", c.getEmail());
        result.put("status", c.getStatus().name());
        return ResponseEntity.ok(result);
    }

    // ── Get Active Connection for Customer ───────────────────────
    @GetMapping("/{customerCode}/connection")
    public ResponseEntity<Map<String, Object>> getConnection(
            @PathVariable(value = "customerCode") String customerCode) {

        String sql =
            "SELECT cc.connection_id, cc.plan_id, cc.service_area_id, " +
            "p.plan_name, p.monthly_price, p.speed_label, p.olt_type, " +
            "sa.pincode, o.olt_code, s.splitter_number, pt.port_number " +
            "FROM customer_connections cc " +
            "JOIN customers c ON c.customer_id = cc.customer_id " +
            "JOIN plans p ON p.plan_id = cc.plan_id " +
            "JOIN ports pt ON pt.port_id = cc.port_id " +
            "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
            "JOIN olts o ON o.olt_id = s.olt_id " +
            "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
            "WHERE c.customer_code = ? AND cc.connection_status = 'ACTIVE' " +
            "LIMIT 1";

        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, customerCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return ResponseEntity.ok(Collections.emptyMap());

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("connectionId", rs.getLong("connection_id"));
                row.put("planId", rs.getLong("plan_id"));
                row.put("serviceAreaId", rs.getLong("service_area_id"));
                row.put("planName", rs.getString("plan_name"));
                row.put("monthlyPrice", rs.getDouble("monthly_price"));
                row.put("speedLabel", rs.getString("speed_label"));
                row.put("oltType", rs.getString("olt_type"));
                row.put("pincode", rs.getString("pincode"));
                row.put("oltCode", rs.getString("olt_code"));
                row.put("splitterNumber", rs.getInt("splitter_number"));
                row.put("portNumber", rs.getInt("port_number"));
                return ResponseEntity.ok(row);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Get Bills for Customer ───────────────────────────────────
    @GetMapping("/{customerCode}/bills")
    public ResponseEntity<List<Map<String, Object>>> getBills(
            @PathVariable(value = "customerCode") String customerCode) {

        Customer c = customerService.lookupCustomerByCode(customerCode);
        if (c == null) return ResponseEntity.notFound().build();

        List<Bill> bills = billService.getBillsForCustomer(c.getCustomerId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Bill b : bills) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("billId", b.getBillId());
            row.put("billNo", b.getBillNo());
            row.put("connectionId", b.getConnectionId());
            row.put("billDate", b.getBillDate().toString());
            row.put("dueDate", b.getDueDate().toString());
            row.put("planCharge", b.getPlanCharge());
            row.put("gstAmount", b.getGstAmount());
            row.put("totalAmount", b.getTotalAmount());
            row.put("billStatus", b.getBillStatus().name());
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // ── Generate Bill ────────────────────────────────────────────
    @PostMapping("/{customerCode}/bills/generate")
    public ResponseEntity<Map<String, Object>> generateBill(
            @PathVariable(value = "customerCode") String customerCode) {

        Map<String, Object> result = new HashMap<>();
        try {
            Customer c = customerService.lookupCustomerByCode(customerCode);
            if (c == null) {
                result.put("message", "Customer not found.");
                return ResponseEntity.badRequest().body(result);
            }

            CustomerConnection conn = connectionService.getActiveConnectionByCustomerCode(customerCode);
            if (conn == null) {
                result.put("message", "No active connection found.");
                return ResponseEntity.badRequest().body(result);
            }

            Plan plan = planService.findPlanById(conn.getPlanId());
            if (plan == null) {
                result.put("message", "Plan not found.");
                return ResponseEntity.badRequest().body(result);
            }

            Bill bill = billService.generateMonthlyBill(c, conn.getConnectionId(), plan);

            result.put("message", "Bill generated successfully.");
            result.put("billId", bill.getBillId());
            result.put("billNo", bill.getBillNo());
            result.put("billDate", bill.getBillDate().toString());
            result.put("dueDate", bill.getDueDate().toString());
            result.put("planCharge", bill.getPlanCharge());
            result.put("gstAmount", bill.getGstAmount());
            result.put("totalAmount", bill.getTotalAmount());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to generate bill.");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ── Mark Bill as Paid ────────────────────────────────────────
    @PostMapping("/bills/{billId}/pay")
    public ResponseEntity<Map<String, String>> payBill(
            @PathVariable(value = "billId") Long billId) {

        Map<String, String> result = new HashMap<>();
        try {
            billService.payBill(billId);
            result.put("message", "Bill marked as PAID.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage() != null ? e.getMessage() : "Failed to pay bill.");
            return ResponseEntity.badRequest().body(result);
        }
    }


}
