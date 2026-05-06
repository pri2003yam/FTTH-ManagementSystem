package ftth.repository;

import ftth.config.DbConnection;
import ftth.model.Plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class PlanRepository {

public boolean insertPlan(Plan plan) {

    String sql =
        "INSERT INTO plans " +
        "(plan_code, plan_name, speed_label, data_limit_label, ott_count, " +
        " monthly_price, olt_type, is_active) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        String autoCode = plan.getPlanName().toUpperCase().replaceAll("\\s+", "_");
        ps.setString(1, autoCode);
        ps.setString(2, plan.getPlanName());
        ps.setString(3, plan.getSpeedLabel());
        ps.setString(4, plan.getDataLimitLabel());
        ps.setInt(5, plan.getOttCount());
        ps.setBigDecimal(6, plan.getMonthlyPrice());        // ✅ BigDecimal
        ps.setString(7, plan.getOltType());
        ps.setBoolean(8, plan.isActive());

        return ps.executeUpdate() > 0;

    } catch (SQLIntegrityConstraintViolationException e) {
        System.out.println(
            "Plan with name '" + plan.getPlanName() + "' already exists."
        );
        return false;

    } catch (SQLException e) {
        throw new RuntimeException("Error adding plan", e);
    }
}
public List<Plan> findAllPlans() {
        String sql =
            "SELECT plan_id, plan_name, speed_label, data_limit_label, ott_count, monthly_price, olt_type, is_active, created_at " +
            "FROM plans ORDER BY plan_id";

        List<Plan> plans = new ArrayList<>();
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plans.add(toPlan(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading plans", e);
        }
        return plans;
    }

public List<Plan> findActivePlans() {

    String sql =
        "SELECT p.plan_id, p.plan_name, p.speed_label, p.data_limit_label, " +
        "       p.ott_count, p.monthly_price, p.olt_type, p.is_active, p.created_at, " +
        "       COUNT(cc.connection_id) AS customer_count " +
        "FROM plans p " +
        "LEFT JOIN customer_connections cc " +
        "  ON p.plan_id = cc.plan_id " +
        " AND cc.connection_status = 'ACTIVE' " +
        "WHERE p.is_active = TRUE " +
        "GROUP BY p.plan_id, p.plan_name, p.speed_label, p.data_limit_label, " +
        "         p.ott_count, p.monthly_price, p.olt_type, p.is_active, p.created_at " +
        "ORDER BY p.plan_id";

    List<Plan> plans = new ArrayList<>();

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Plan p = toPlan(rs);

            // ✅ set derived / runtime-only field
            p.setCustomerCount(rs.getInt("customer_count"));

            plans.add(p);
        }

    } catch (SQLException e) {
        throw new RuntimeException("Error loading active plans", e);
    }

    return plans;
}

public Plan findPlanById(long id) {
        String sql =
            "SELECT plan_id, plan_name, speed_label, data_limit_label, ott_count, monthly_price, olt_type, is_active, created_at " +
            "FROM plans WHERE plan_id = ?";

        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toPlan(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding plan", e);
        }
        return null;
    }
private Plan toPlan(ResultSet rs) throws SQLException {

    return new Plan(
        rs.getLong("plan_id"),
        rs.getString("plan_name"),
        rs.getString("speed_label"),
        rs.getString("data_limit_label"),
        rs.getInt("ott_count"),
        rs.getBigDecimal("monthly_price"),
        rs.getString("olt_type"),
        rs.getBoolean("is_active"),
        rs.getTimestamp("created_at").toLocalDateTime()
    );
}

public boolean updatePlan(long planId, Plan updatedPlan) {

    String sql =
        "UPDATE plans " +
        "SET plan_name = ?, " +
        "    speed_label = ?, " +
        "    data_limit_label = ?, " +
        "    ott_count = ?, " +
        "    monthly_price = ?, " +
        "    olt_type = ?, " +
        "    is_active = ? " +
        "WHERE plan_id = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, updatedPlan.getPlanName());
        ps.setString(2, updatedPlan.getSpeedLabel());
        ps.setString(3, updatedPlan.getDataLimitLabel());
        ps.setInt(4, updatedPlan.getOttCount());
        ps.setBigDecimal(5, updatedPlan.getMonthlyPrice()); // ✅ FIXED
        ps.setString(6, updatedPlan.getOltType());
        ps.setBoolean(7, updatedPlan.isActive());
        ps.setLong(8, planId);

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        throw new RuntimeException("Error updating plan with id=" + planId, e);
    }
}

public boolean deletePlan(long id) {
        String checkActiveSql = "SELECT COUNT(*) FROM customer_connections WHERE plan_id = ? AND connection_status = 'ACTIVE'";
        String checkAllSql    = "SELECT COUNT(*) FROM customer_connections WHERE plan_id = ?";
        String deleteSql      = "DELETE FROM plans WHERE plan_id = ?";

        try (Connection con = DbConnection.getConnection()) {
            PreparedStatement checkPs = con.prepareStatement(checkActiveSql);
            checkPs.setLong(1, id);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                return false;
            }

            PreparedStatement checkAllPs = con.prepareStatement(checkAllSql);
            checkAllPs.setLong(1, id);
            ResultSet rsAll = checkAllPs.executeQuery();
            rsAll.next();
            if (rsAll.getInt(1) > 0) {
                return false;
            }

            PreparedStatement deletePs = con.prepareStatement(deleteSql);
            deletePs.setLong(1, id);
            return deletePs.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting plan", e);
        }
    }

public boolean togglePlanStatus(long planId, boolean newStatus) {

    String sql = "UPDATE plans SET is_active = ? WHERE plan_id = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setBoolean(1, newStatus);
        ps.setLong(2, planId);

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        throw new RuntimeException("Error toggling plan status for planId=" + planId, e);
    }
}
   

     private static final String FIND_BY_ID_SQL =
    "SELECT plan_id, plan_name, speed_label, data_limit_label, " +
    "ott_count, monthly_price, olt_type, is_active, created_at " +
    "FROM plans WHERE plan_id = ?";

public Plan findById(Long planId) {

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {

        ps.setLong(1, planId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return toPlan(rs);   // ✅ use correct mapper
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching plan with ID: " + planId, e
        );
    }

    return null;
}
public String findOltTypeByPlanId(long planId) {

    String sql =
        "SELECT olt_type " +
        "FROM plans " +
        "WHERE plan_id = ? " +
        "  AND is_active = 1";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, planId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("olt_type");
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching OLT type for plan " + planId, e
        );
    }

    return null;
}

    private static final String FIND_PLANS_WITH_COUNTS_SQL =
        "SELECT p.plan_id, p.plan_name, p.speed_label, p.data_limit_label, " +
        "       p.ott_count, p.monthly_price, p.olt_type, p.is_active, " +
        "       COUNT(cc.connection_id) AS customer_count " +
        "FROM plans p " +
        "LEFT JOIN customer_connections cc " +
        "  ON p.plan_id = cc.plan_id " +
        " AND cc.connection_status = 'ACTIVE' " +
        "GROUP BY p.plan_id, p.plan_name, p.speed_label, p.data_limit_label, " +
        "         p.ott_count, p.monthly_price, p.olt_type, p.is_active " +
        "ORDER BY p.plan_id";

    public List<Plan> findPlansWithCustomerCount() {

        List<Plan> plans = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_PLANS_WITH_COUNTS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Plan p = new Plan();
                p.setPlanId(rs.getLong("plan_id"));
                p.setPlanName(rs.getString("plan_name"));
                p.setSpeedLabel(rs.getString("speed_label"));
                p.setDataLimitLabel(rs.getString("data_limit_label"));
                p.setOttCount(rs.getInt("ott_count"));
                p.setMonthlyPrice(rs.getBigDecimal("monthly_price"));
                p.setOltType(rs.getString("olt_type"));
                p.setActive(rs.getBoolean("is_active"));

                // ✅ derived field
                p.setCustomerCount(rs.getInt("customer_count"));

                plans.add(p);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching plans with customer counts", e);
        }

        return plans;
    }

}


          


   
