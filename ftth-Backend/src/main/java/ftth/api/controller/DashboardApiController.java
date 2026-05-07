package ftth.api.controller;

import ftth.config.DbConnection;
import ftth.model.dtos.CapacityRow;
import ftth.repository.CapacityInventoryRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final CapacityInventoryRepository capacityRepo;

    public DashboardApiController() {
        this.capacityRepo = new CapacityInventoryRepository();
    }

    // ══════════════════════════════════════════════
    // ADMIN DASHBOARD
    // ══════════════════════════════════════════════
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DbConnection.getConnection()) {

            // Total customers
            result.put("totalCustomers", countQuery(con, "SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'"));

            // Active connections
            result.put("activeConnections", countQuery(con, "SELECT COUNT(*) FROM customer_connections WHERE connection_status = 'ACTIVE'"));

            // Available ports
            result.put("availablePorts", countQuery(con, "SELECT COUNT(*) FROM ports WHERE port_status = 'AVAILABLE'"));

            // Monthly revenue
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(p.monthly_price), 0) FROM customer_connections cc " +
                    "JOIN plans p ON p.plan_id = cc.plan_id WHERE cc.connection_status = 'ACTIVE'");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                result.put("totalRevenue", rs.getDouble(1));
            }

            // New connections this week
            result.put("newConnectionsThisWeek", countQuery(con,
                "SELECT COUNT(*) FROM customer_connections WHERE activated_on >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)"));

            // Disconnects this week
            result.put("disconnectsThisWeek", countQuery(con,
                "SELECT COUNT(*) FROM customer_connections WHERE connection_status = 'DISCONNECTED' AND disconnected_on >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)"));

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════
    // CONNECTIONS CHART (last 30 days)
    // ══════════════════════════════════════════════
    @GetMapping("/connections-chart")
    public ResponseEntity<Map<String, Object>> connectionsChart() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection con = DbConnection.getConnection()) {
            result.put("newConnections", countQuery(con,
                "SELECT COUNT(*) FROM email_logs WHERE email_type = 'ORDER_CONFIRMATION' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"));
            result.put("changePlan", countQuery(con,
                "SELECT COUNT(*) FROM email_logs WHERE email_type = 'PLAN_CHANGE' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"));
            result.put("move", countQuery(con,
                "SELECT COUNT(*) FROM email_logs WHERE email_type = 'SERVICE_MOVE' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"));
            result.put("disconnect", countQuery(con,
                "SELECT COUNT(*) FROM email_logs WHERE email_type = 'DISCONNECT' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"));
            result.put("total", countQuery(con,
                "SELECT COUNT(*) FROM customer_connections WHERE connection_status = 'ACTIVE'"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════
    // CAPACITY SUMMARY (used by admin alerts)
    // ══════════════════════════════════════════════
    @GetMapping("/capacity-summary")
    public ResponseEntity<List<Map<String, Object>>> capacitySummary() {
        List<CapacityRow> rows = capacityRepo.fetchAllCapacity();
        List<Map<String, Object>> result = new ArrayList<>();

        for (CapacityRow r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("oltCode", r.getOltType() + "-" + r.getPincode());
            m.put("usagePercent", r.getUtilization());
            m.put("totalPorts", r.getTotalPorts());
            m.put("usedPorts", r.getUsedPorts());
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════
    // CSR DASHBOARD
    // ══════════════════════════════════════════════
    @GetMapping("/csr")
    public ResponseEntity<Map<String, Object>> csrDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DbConnection.getConnection()) {

            result.put("totalCustomers", countQuery(con, "SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'"));

            result.put("newCustomersToday", countQuery(con,
                "SELECT COUNT(*) FROM customers WHERE DATE(created_at) = CURDATE()"));

            result.put("pendingRequests", 0); // placeholder — no pending requests table yet

            result.put("activeConnections", countQuery(con,
                "SELECT COUNT(*) FROM customer_connections WHERE connection_status = 'ACTIVE'"));

            // Top plans
            List<Map<String, Object>> topPlans = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT p.plan_name, p.monthly_price, COUNT(cc.connection_id) AS active_count " +
                    "FROM plans p LEFT JOIN customer_connections cc ON cc.plan_id = p.plan_id AND cc.connection_status = 'ACTIVE' " +
                    "WHERE p.is_active = TRUE GROUP BY p.plan_id ORDER BY active_count DESC LIMIT 5");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> plan = new LinkedHashMap<>();
                    plan.put("planName", rs.getString("plan_name"));
                    plan.put("monthlyPrice", rs.getDouble("monthly_price"));
                    plan.put("activeCount", rs.getInt("active_count"));
                    topPlans.add(plan);
                }
            }
            result.put("topPlans", topPlans);

            // Recent activity
            List<Map<String, Object>> recentActivity = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT el.log_id AS id, " +
                    "CASE el.email_type " +
                    "  WHEN 'ORDER_CONFIRMATION' THEN 'New Connection' " +
                    "  WHEN 'PLAN_CHANGE' THEN 'Plan Change' " +
                    "  WHEN 'SERVICE_MOVE' THEN 'Service Move' " +
                    "  WHEN 'DISCONNECT' THEN 'Disconnect' " +
                    "  ELSE el.email_type END AS type, " +
                    "CONCAT(c.full_name, ' - ', p.plan_name) AS description, " +
                    "el.created_at AS timestamp " +
                    "FROM email_logs el " +
                    "JOIN customers c ON c.customer_id = el.customer_id " +
                    "JOIN customer_connections cc ON cc.customer_id = el.customer_id " +
                    "JOIN plans p ON p.plan_id = cc.plan_id " +
                    "WHERE el.email_type IN ('ORDER_CONFIRMATION','PLAN_CHANGE','SERVICE_MOVE','DISCONNECT') " +
                    "AND cc.connection_id = (SELECT MAX(cc2.connection_id) FROM customer_connections cc2 WHERE cc2.customer_id = el.customer_id) " +
                    "ORDER BY el.created_at DESC LIMIT 10");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> activity = new LinkedHashMap<>();
                    activity.put("id", rs.getLong("id"));
                    activity.put("type", rs.getString("type"));
                    activity.put("description", rs.getString("description"));
                    activity.put("timestamp", rs.getTimestamp("timestamp").toString());
                    recentActivity.add(activity);
                }
            }
            result.put("recentActivity", recentActivity);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════
    // MAINTENANCE DASHBOARD
    // ══════════════════════════════════════════════
    @GetMapping("/maint")
    public ResponseEntity<Map<String, Object>> maintDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DbConnection.getConnection()) {

            // Down connections (disconnected recently = potential issues)
            result.put("downConnections", countQuery(con,
                "SELECT COUNT(*) FROM customer_connections WHERE connection_status = 'DISCONNECTED' AND disconnected_on >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)"));

            // Active issues — OLTs at high capacity
            List<CapacityRow> rows = capacityRepo.fetchAllCapacity();
            int activeIssues = 0;
            List<Map<String, Object>> issues = new ArrayList<>();

            for (CapacityRow r : rows) {
                if (r.getUtilization() >= 80) {
                    activeIssues++;
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("id", r.getOltId());
                    issue.put("area", "Pincode " + r.getPincode());
                    issue.put("description", r.getOltType() + " OLT at " + r.getUtilization() + "% capacity");
                    issue.put("severity", r.getUtilization() >= 95 ? "HIGH" : "MEDIUM");
                    issue.put("status", "OPEN");
                    issues.add(issue);
                }
            }

            result.put("activeIssues", activeIssues);
            result.put("pendingTasks", activeIssues); // same as active issues for now
            result.put("resolvedToday", 0); // placeholder
            result.put("issues", issues);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ── Helper ──
    private int countQuery(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
