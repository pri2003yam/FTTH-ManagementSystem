package ftth.repository;

import ftth.config.DbConnection;
import ftth.model.CustomerConnection;
import ftth.model.enums.ConnectionStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerConnectionRepository {

public boolean createConnection(String customerName, long planId, long portId, long serviceAreaId) {
        String sql = "INSERT INTO customer_connections " +
            "(customer_name, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day) " +
            "VALUES (?, ?, ?, ?, 'ACTIVE', CURDATE(), 10)";

        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerName);
            ps.setLong(2, planId);
            ps.setLong(3, portId);
            ps.setLong(4, serviceAreaId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error creating connection: " + e.getMessage());
            return false;
        }
    }
    private static final String INSERT_SQL =
        "INSERT INTO customer_connections (" +
        "customer_id, plan_id, port_id, service_area_id, " +
        "connection_status, activated_on, billing_day, created_by" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

public void insert(CustomerConnection connection) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, connection.getCustomerId());
            ps.setLong(2, connection.getPlanId());
            ps.setLong(3, connection.getPortId());
            ps.setLong(4, connection.getServiceAreaId());
            ps.setString(5, connection.getConnectionStatus().name());
            ps.setDate(6, Date.valueOf(connection.getActivatedOn()));
            ps.setInt(7, connection.getBillingDay());
            ps.setLong(8, connection.getCreatedBy());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    connection.setConnectionId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting customer connection", e);
        }
    }

    // ===============================
    // READ (by connection_id)
    // ===============================
    private static final String FIND_BY_ID_SQL =
        "SELECT * FROM customer_connections WHERE connection_id = ?";

public CustomerConnection findById(Long connectionId) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {

            ps.setLong(1, connectionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching connection", e);
        }

        return null;
    }
    /**
 * Find active connection using customer code.
 */
public CustomerConnection findActiveByCustomerCode(String customerCode) {

    final String SQL =
        "SELECT cc.* " +
        "FROM customer_connections cc " +
        "JOIN customers c ON cc.customer_id = c.customer_id " +
        "WHERE c.customer_code = ? " +
        "AND cc.connection_status = 'ACTIVE'";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(SQL)) {

        ps.setString(1, customerCode);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs); // existing mapper
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching active connection for customer code: " + customerCode, e
        );
    }

    return null; // no active connection
}

    // ===============================
    // READ (active connection by customer_id)
    // ===============================
    private static final String FIND_ACTIVE_BY_CUSTOMER_ID_SQL =
        "SELECT * FROM customer_connections WHERE customer_id = ? AND connection_status = 'ACTIVE' LIMIT 1";

    public CustomerConnection findActiveByCustomerId(Long customerId) {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE_BY_CUSTOMER_ID_SQL)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking active connection for customer", e);
        }
        return null;
    }

    // ===============================
    // READ (by customer)
    // ===============================
    private static final String FIND_BY_CUSTOMER_SQL =
        "SELECT * FROM customer_connections WHERE customer_id = ?";

public List<CustomerConnection> findByCustomerId(Long customerId) {

        List<CustomerConnection> list = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CUSTOMER_SQL)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching connections", e);
        }

        return list;
    }
    private static final String UPDATE_LOCATION_SQL =
    "UPDATE customer_connections " +
    "SET service_area_id = ?, port_id = ?, updated_by = ? " +
    "WHERE connection_id = ? AND connection_status = 'ACTIVE'";
public void updateLocation(Long connectionId,
                           Long newServiceAreaId,
                           Long newPortId,
                           Long updatedBy) {

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps =
             conn.prepareStatement(UPDATE_LOCATION_SQL)) {

        ps.setLong(1, newServiceAreaId);
        ps.setLong(2, newPortId);
        ps.setLong(3, updatedBy);
        ps.setLong(4, connectionId);

        ps.executeUpdate();

    } catch (SQLException e) {
        throw new RuntimeException("Error moving connection", e);
    }
}

    // ===============================
    // UPDATE (Change Plan)
    // ===============================
    private static final String UPDATE_PLAN_SQL =
        "UPDATE customer_connections " +
        "SET plan_id = ?, updated_by = ? " +
        "WHERE connection_id = ? AND connection_status = 'ACTIVE'";

public void updatePlan(Long connectionId, Long newPlanId, Long updatedBy) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_PLAN_SQL)) {

            ps.setLong(1, newPlanId);
            ps.setLong(2, updatedBy);
            ps.setLong(3, connectionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating plan", e);
        }
    }

    // ===============================
    // UPDATE (Change Port)
    // ===============================
    private static final String UPDATE_PORT_SQL =
        "UPDATE customer_connections " +
        "SET port_id = ?, updated_by = ? " +
        "WHERE connection_id = ? AND connection_status = 'ACTIVE'";

public void updatePort(Long connectionId, Long newPortId, Long updatedBy) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_PORT_SQL)) {

            ps.setLong(1, newPortId);
            ps.setLong(2, updatedBy);
            ps.setLong(3, connectionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error updating port", e);
        }
    }

    // ===============================
    // DELETE (SOFT) → DISCONNECT
    // ===============================
    private static final String DISCONNECT_SQL =
        "UPDATE customer_connections " +
        "SET connection_status = 'DISCONNECTED', " +
        "disconnected_on = ?, updated_by = ? " +
        "WHERE connection_id = ? AND connection_status = 'ACTIVE'";

public void disconnect(Long connectionId, LocalDate date, Long updatedBy) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DISCONNECT_SQL)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setLong(2, updatedBy);
            ps.setLong(3, connectionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error disconnecting connection", e);
        }
    }

    // ===============================
    // MAPPER
    // ===============================
private CustomerConnection mapRow(ResultSet rs) throws SQLException {

        return new CustomerConnection(
            rs.getLong("connection_id"),
            rs.getLong("customer_id"),
            rs.getLong("plan_id"),
            rs.getLong("port_id"),
            rs.getLong("service_area_id"),
            ConnectionStatus.valueOf(rs.getString("connection_status")),
            rs.getDate("activated_on").toLocalDate(),
            rs.getDate("disconnected_on") != null
                ? rs.getDate("disconnected_on").toLocalDate()
                : null,
            rs.getInt("billing_day"),
            rs.getLong("created_by"),
            rs.getLong("updated_by"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }
public String[] findConnection(long connectionId) {

    String sql =
        "SELECT cc.connection_id, c.full_name, sa.pincode, " +
        "p.plan_name, p.monthly_price, cc.connection_status, " +
        "o.olt_code, o.olt_type, s.splitter_number, pt.port_number, cc.port_id " +
        "FROM customer_connections cc " +
        "JOIN customers c ON c.customer_id = cc.customer_id " +
        "JOIN plans p ON p.plan_id = cc.plan_id " +
        "JOIN ports pt ON pt.port_id = cc.port_id " +
        "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
        "JOIN olts o ON o.olt_id = s.olt_id " +
        "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
        "WHERE cc.connection_id = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, connectionId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new String[] {
                    String.valueOf(rs.getLong("connection_id")),   // [0]
                    rs.getString("full_name"),                     // [1]
                    rs.getString("pincode"),                       // [2]
                    rs.getString("plan_name"),                     // [3]
                    String.valueOf(rs.getDouble("monthly_price")), // [4]
                    rs.getString("connection_status"),             // [5]
                    rs.getString("olt_code"),                      // [6]
                    String.valueOf(rs.getInt("splitter_number")),  // [7]
                    String.valueOf(rs.getInt("port_number")),      // [8]
                    String.valueOf(rs.getLong("port_id")),         // [9]
                    rs.getString("olt_type")                       // [10]
                };
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error finding connection with id=" + connectionId,
            e
        );
    }

    return null;
}


    public Long findActivePlanIdByCustomerCode(String customerCode) {

    String sql =
        "SELECT cc.plan_id " +
        "FROM customer_connections cc " +
        "JOIN customers c ON c.customer_id = cc.customer_id " +
        "WHERE c.customer_code = ? " +
        "AND cc.connection_status = 'ACTIVE' " +
        "ORDER BY cc.connection_id DESC " +
        "LIMIT 1";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, customerCode);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("plan_id");
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error finding active plan for customerCode=" + customerCode,
            e
        );
    }

    return null;
}
public Long findActiveConnectionIdByCustomerCode(String customerCode) {

    String sql =
        "SELECT cc.connection_id " +
        "FROM customer_connections cc " +
        "JOIN customers c " +
        "  ON cc.customer_id = c.customer_id " +
        "WHERE c.customer_code = ? " +
        "  AND cc.connection_status = 'ACTIVE'";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, customerCode);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("connection_id");
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching connection for customer " + customerCode, e
        );
    }

    return null; // No active connection
}


    public boolean updateConnectionPlan(String customerCode, long newPlanId) {

    String sql =
        "UPDATE customer_connections cc " +
        "JOIN customers c ON c.customer_id = cc.customer_id " +
        "SET cc.plan_id = ? " +
        "WHERE c.customer_code = ? " +
        "AND cc.connection_status = 'ACTIVE'";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, newPlanId);
        ps.setString(2, customerCode);

        return ps.executeUpdate() > 0;

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error updating plan for customerCode=" + customerCode,
            e
        );
    }
}
    public String[] findActiveConnectionByCustomerCode(String customerCode) {

    String sql =
        "SELECT cc.connection_id, c.full_name, sa.pincode, " +
        "p.plan_name, p.monthly_price, cc.connection_status, " +
        "o.olt_code, o.olt_type, s.splitter_number, pt.port_number, cc.port_id " +
        "FROM customer_connections cc " +
        "JOIN customers c ON c.customer_id = cc.customer_id " +
        "JOIN plans p ON p.plan_id = cc.plan_id " +
        "JOIN ports pt ON pt.port_id = cc.port_id " +
        "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
        "JOIN olts o ON o.olt_id = s.olt_id " +
        "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
        "WHERE c.customer_code = ? " +
        "AND cc.connection_status = 'ACTIVE' " +
        "ORDER BY cc.connection_id DESC " +
        "LIMIT 1";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, customerCode);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new String[] {
                    String.valueOf(rs.getLong("connection_id")),   // [0]
                    rs.getString("full_name"),                     // [1]
                    rs.getString("pincode"),                       // [2]
                    rs.getString("plan_name"),                     // [3]
                    String.valueOf(rs.getDouble("monthly_price")), // [4]
                    rs.getString("connection_status"),             // [5]
                    rs.getString("olt_code"),                      // [6]
                    String.valueOf(rs.getInt("splitter_number")),  // [7]
                    String.valueOf(rs.getInt("port_number")),      // [8]
                    String.valueOf(rs.getLong("port_id")),         // [9]
                    rs.getString("olt_type")                       // [10]
                };
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error finding active connection for customerCode=" + customerCode,
            e
        );
    }

    return null;
}

 public boolean moveConnection(long connectionId, long oldPortId, long newPortId, long newServiceAreaId) {
        try (Connection con = DbConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Free old port
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ports SET port_status = 'AVAILABLE' WHERE port_id = ?")) {
                    ps.setLong(1, oldPortId);
                    ps.executeUpdate();
                }
                // Assign new port
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ports SET port_status = 'ASSIGNED' WHERE port_id = ?")) {
                    ps.setLong(1, newPortId);
                    ps.executeUpdate();
                }
                // Update connection
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE customer_connections SET port_id = ?, service_area_id = ? WHERE connection_id = ?")) {
                    ps.setLong(1, newPortId);
                    ps.setLong(2, newServiceAreaId);
                    ps.setLong(3, connectionId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Error moving connection: " + e.getMessage());
            return false;
        }
    }

public void listActiveConnections() {

    String sql =
    "SELECT cc.connection_id, c.customer_code, c.full_name, sa.pincode, " +
    "p.plan_name, p.monthly_price, cc.connection_status, " +
    "o.olt_code, o.olt_type, s.splitter_number, pt.port_number " +
    "FROM customer_connections cc " +
    "JOIN customers c ON c.customer_id = cc.customer_id " +
    "JOIN plans p ON p.plan_id = cc.plan_id " +
    "JOIN ports pt ON pt.port_id = cc.port_id " +
    "JOIN splitters s ON s.splitter_id = pt.splitter_id " +
    "JOIN olts o ON o.olt_id = s.olt_id " +
    "JOIN service_areas sa ON sa.service_area_id = cc.service_area_id " +
    "WHERE cc.connection_status = 'ACTIVE' " +
    "ORDER BY cc.connection_id";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        boolean found = false;

        System.out.println("\n--- Active Connections ---");
        System.out.printf(
    "%-5s %-12s %-18s %-8s %-12s %-10s %-8s %-25s%n",
    "ID", "CustCode", "Customer", "Pincode", "Plan", "Price", "OLT", "Port"
);
        System.out.println("-".repeat(110));

        while (rs.next()) {
           System.out.printf(
    "%-5d %-12s %-18s %-8s %-12s Rs.%-7.0f %-8s %s/Spl%d/Port%d%n",
    rs.getLong("connection_id"),
    rs.getString("customer_code"),   // ✅ new 2nd column
    rs.getString("full_name"),       // ✅ name stays
    rs.getString("pincode"),
    rs.getString("plan_name"),
    rs.getDouble("monthly_price"),
    rs.getString("olt_type"),
    rs.getString("olt_code"),
    rs.getInt("splitter_number"),
    rs.getInt("port_number")
);
            found = true;
        }

        if (!found) {
            System.out.println("No active connections.");
        }

        System.out.println();

    } catch (SQLException e) {
        throw new RuntimeException("Error listing active connections", e);
    }
}


public boolean disconnectCustomer(long connectionId) {
        try (Connection con = DbConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String findSql = "SELECT port_id FROM customer_connections WHERE connection_id = ? AND connection_status = 'ACTIVE'";
                PreparedStatement findPs = con.prepareStatement(findSql);
                findPs.setLong(1, connectionId);
                ResultSet rs = findPs.executeQuery();
                if (!rs.next()) {
                    System.out.println("Connection not found or already disconnected.");
                    con.rollback();
                    return false;
                }
                long portId = rs.getLong("port_id");

                PreparedStatement disconnectPs = con.prepareStatement(
                    "UPDATE customer_connections SET connection_status = 'DISCONNECTED', disconnected_on = CURDATE() WHERE connection_id = ?");
                disconnectPs.setLong(1, connectionId);
                disconnectPs.executeUpdate();

                PreparedStatement freePs = con.prepareStatement(
                    "UPDATE ports SET port_status = 'AVAILABLE' WHERE port_id = ?");
                freePs.setLong(1, portId);
                freePs.executeUpdate();

                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Error disconnecting customer: " + e.getMessage());
            return false;
        }
    }
}







