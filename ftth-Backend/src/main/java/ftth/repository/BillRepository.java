package ftth.repository;


import ftth.config.DbConnection;
import ftth.model.Bill;
import ftth.model.enums.BillStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillRepository {
    // ===============================
    // CREATE
    // ===============================
    private static final String INSERT_SQL =
    "INSERT INTO bills (" +
    "bill_no, customer_id, connection_id, bill_date, due_date, " +
    "plan_charge, gst_amount, total_amount, bill_status" +
    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

public void insert(Bill bill) {

    try (Connection conn = DbConnection.getConnection();   // ✅ FIXED
         PreparedStatement ps =
             conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, bill.getBillNo());
        ps.setLong(2, bill.getCustomerId());
        ps.setLong(3, bill.getConnectionId());
        ps.setDate(4, Date.valueOf(bill.getBillDate()));
        ps.setDate(5, Date.valueOf(bill.getDueDate()));
        ps.setBigDecimal(6, bill.getPlanCharge());
        ps.setBigDecimal(7, bill.getGstAmount());
        ps.setBigDecimal(8, bill.getTotalAmount());
        ps.setString(9, bill.getBillStatus().name());

        ps.executeUpdate();

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                bill.setBillId(rs.getLong(1));
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException("Error inserting bill", e);
    }
}
public long getLastBillSequence() {

    String sql = "SELECT bill_no FROM bills ORDER BY bill_id DESC LIMIT 1";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        if (rs.next()) {
            String billNo = rs.getString("bill_no");
            // Example: BILL-2026-000123
            String[] parts = billNo.split("-");
            return Long.parseLong(parts[2]); // 123
        }

    } catch (Exception e) {
        throw new RuntimeException("Failed to read last bill number", e);
    }

    return 0; // no bills yet
}
    // ===============================
    // READ — BY BILL ID
    // ===============================
    private static final String FIND_BY_ID_SQL =
    "SELECT * FROM bills WHERE bill_id = ?";

public Bill findById(Long billId) {

    try (Connection conn = DbConnection.getConnection();   // ✅ FIXED
         PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {

        ps.setLong(1, billId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);   // ✅ use existing Bill mapper
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching bill with ID: " + billId, e
        );
    }

    return null;
}
    // ===============================
    // READ — BY CUSTOMER
    // ===============================
    private static final String FIND_BY_CUSTOMER_SQL =
    "SELECT * FROM bills WHERE customer_id = ? ORDER BY bill_date DESC";

public List<Bill> findByCustomerId(Long customerId) {

    List<Bill> bills = new ArrayList<>();

    try (Connection conn = DbConnection.getConnection();   // ✅ FIXED
         PreparedStatement ps =
             conn.prepareStatement(FIND_BY_CUSTOMER_SQL)) {

        ps.setLong(1, customerId);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bills.add(mapRow(rs));   // ✅ correct mapper
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException(
            "Error fetching bills for customerId=" + customerId, e
        );
    }

    return bills;
}

    // ===============================
    // UPDATE — MARK AS PAID
    // ===============================
    private static final String MARK_PAID_SQL =
    "UPDATE bills SET bill_status = 'PAID' WHERE bill_id = ?";

public void markAsPaid(Long billId) {

    try (Connection conn = DbConnection.getConnection();   // ✅ FIXED
         PreparedStatement ps =
             conn.prepareStatement(MARK_PAID_SQL)) {

        ps.setLong(1, billId);
        ps.executeUpdate();

    } catch (SQLException e) {
        throw new RuntimeException("Error marking bill as PAID", e);
    }
}
    // ===============================
    // UPDATE — MARK AS OVERDUE
    // ===============================
    private static final String MARK_OVERDUE_SQL =
    "UPDATE bills SET bill_status = 'OVERDUE' " +
    "WHERE bill_id = ? AND bill_status = 'GENERATED'";

public void markAsOverdue(Long billId) {

    try (Connection conn = DbConnection.getConnection();   // ✅ FIXED
         PreparedStatement ps =
             conn.prepareStatement(MARK_OVERDUE_SQL)) {

        ps.setLong(1, billId);
        ps.executeUpdate();

    } catch (SQLException e) {
        throw new RuntimeException("Error marking bill as OVERDUE", e);
    }
}
    // ===============================
    // CHECK — DUPLICATE BILL
    // ===============================
    public boolean existsByConnectionAndBillDate(long connectionId, LocalDate billDate) {
        String sql = "SELECT COUNT(*) FROM bills WHERE connection_id = ? AND bill_date = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, connectionId);
            ps.setDate(2, Date.valueOf(billDate));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking duplicate bill", e);
        }
    }

    // ===============================
    // DELETE — ❌ NOT ALLOWED
    // ===============================
    // Bills are NEVER deleted (audit + finance rule)

    // ===============================
    // MAPPER
    // ===============================
    private Bill mapRow(ResultSet rs) throws SQLException {

    return new Bill(
        rs.getLong("bill_id"),
        rs.getString("bill_no"),
        rs.getLong("customer_id"),
        rs.getLong("connection_id"),
        rs.getDate("bill_date").toLocalDate(),
        rs.getDate("due_date").toLocalDate(),
        rs.getBigDecimal("plan_charge"),
        rs.getBigDecimal("gst_amount"),
        rs.getBigDecimal("total_amount"),
        BillStatus.valueOf(rs.getString("bill_status")),
        rs.getTimestamp("created_at").toLocalDateTime()
    );
}
    
}



  

