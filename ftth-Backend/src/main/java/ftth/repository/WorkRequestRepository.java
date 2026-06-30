package ftth.repository;

import ftth.config.DbConnection;
import ftth.model.WorkRequest;
import ftth.model.enums.WorkRequestStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkRequestRepository {

    public boolean hasActiveRequest(String pincode, String oltType) {
        String sql = "SELECT COUNT(*) FROM work_requests WHERE pincode = ? AND olt_type = ? AND status IN ('NEW','ACCEPTED','IN_PROGRESS')";
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pincode);
            ps.setString(2, oltType);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long insert(WorkRequest wr) {
        String sql = "INSERT INTO work_requests (pincode, olt_type, action_type, status, raised_by, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, wr.getPincode());
            ps.setString(2, wr.getOltType());
            ps.setString(3, wr.getActionType());
            ps.setString(4, wr.getStatus().name());
            ps.setString(5, wr.getRaisedBy());
            ps.setString(6, wr.getDescription());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updateStatus(long wrId, WorkRequestStatus newStatus, String assignedTo) {
        String sql = "UPDATE work_requests SET status = ?, assigned_to = ? WHERE wr_id = ?";
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, assignedTo);
            ps.setLong(3, wrId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public WorkRequest findById(long wrId) {
        String sql = "SELECT * FROM work_requests WHERE wr_id = ?";
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, wrId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WorkRequest> findAll() {
        String sql = "SELECT * FROM work_requests ORDER BY created_at DESC";
        List<WorkRequest> list = new ArrayList<>();
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<WorkRequest> findByRaisedBy(String username) {
        String sql = "SELECT * FROM work_requests WHERE raised_by = ? ORDER BY created_at DESC";
        List<WorkRequest> list = new ArrayList<>();
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<WorkRequest> findOpenRequests() {
        String sql = "SELECT * FROM work_requests WHERE status IN ('NEW','ACCEPTED','IN_PROGRESS','RESOLVED') ORDER BY created_at DESC";
        List<WorkRequest> list = new ArrayList<>();
        try (Connection con = DbConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private WorkRequest mapRow(ResultSet rs) throws SQLException {
        WorkRequest wr = new WorkRequest();
        wr.setWrId(rs.getLong("wr_id"));
        wr.setPincode(rs.getString("pincode"));
        wr.setOltType(rs.getString("olt_type"));
        wr.setActionType(rs.getString("action_type"));
        wr.setStatus(WorkRequestStatus.valueOf(rs.getString("status")));
        wr.setRaisedBy(rs.getString("raised_by"));
        wr.setAssignedTo(rs.getString("assigned_to"));
        wr.setDescription(rs.getString("description"));
        wr.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        wr.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return wr;
    }
}
