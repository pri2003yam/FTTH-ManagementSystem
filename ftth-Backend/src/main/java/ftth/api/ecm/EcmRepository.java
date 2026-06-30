package ftth.api.ecm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class EcmRepository {

    private final EcmDbConnection ecmDb;

    @Value("${ecm.project.code:Basic PSO1}")
    private String projectCode;

    public EcmRepository(EcmDbConnection ecmDb) {
        this.ecmDb = ecmDb;
    }

    public List<Map<String, Object>> findAllProductOfferings() {
        String sql = "SELECT i.itemcode, i.name, i.description, i.status, i.startdate, i.lastupdateddate " +
                     "FROM ecm.cwpc_item i WHERE i.itemtype = 'ProductOffering' AND i.projectcode = ? " +
                     "ORDER BY i.lastupdateddate DESC";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemCode", rs.getString("itemcode"));
                row.put("name", rs.getString("name"));
                row.put("description", rs.getString("description"));
                row.put("status", rs.getString("status"));
                row.put("startDate", rs.getTimestamp("startdate"));
                row.put("lastUpdated", rs.getTimestamp("lastupdateddate"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching ECM offerings", e);
        }

        for (Map<String, Object> offering : results) {
            String itemCode = (String) offering.get("itemCode");
            offering.put("attributes", findAttributes(itemCode));
            offering.put("charges", findCharges(itemCode));
        }
        return results;
    }

    public Map<String, Object> findProductOffering(String itemCode) {
        String sql = "SELECT i.itemcode, i.name, i.description, i.status, i.startdate, i.lastupdateddate " +
                     "FROM ecm.cwpc_item i WHERE i.itemcode = ? AND i.itemtype = 'ProductOffering' AND i.projectcode = ?";
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemCode", rs.getString("itemcode"));
                row.put("name", rs.getString("name"));
                row.put("description", rs.getString("description"));
                row.put("status", rs.getString("status"));
                row.put("startDate", rs.getTimestamp("startdate"));
                row.put("lastUpdated", rs.getTimestamp("lastupdateddate"));
                row.put("attributes", findAttributes(itemCode));
                row.put("charges", findCharges(itemCode));
                return row;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching ECM offering: " + itemCode, e);
        }
        return null;
    }

    public List<Map<String, String>> findAttributes(String itemCode) {
        String sql = "SELECT itemattributecode, name, defaultvalue FROM ecm.cwpc_itemattribute_v " +
                     "WHERE itemcode = ? AND projectcode = ? AND associationtype = 'pscmUserAttribute' ORDER BY sequence";
        List<Map<String, String>> attrs = new ArrayList<>();
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> attr = new HashMap<>();
                attr.put("code", rs.getString("itemattributecode"));
                attr.put("name", rs.getString("name"));
                attr.put("value", rs.getString("defaultvalue"));
                attrs.add(attr);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching attributes for " + itemCode, e);
        }
        return attrs;
    }

    public List<Map<String, Object>> findCharges(String itemCode) {
        String sql = "SELECT itemchargecode, chargetypecode, name, value, status " +
                     "FROM ecm.cwpc_itemcharge_v WHERE itemcode = ? AND projectcode = ?";
        List<Map<String, Object>> charges = new ArrayList<>();
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> charge = new HashMap<>();
                charge.put("chargeCode", rs.getString("itemchargecode"));
                charge.put("chargeType", rs.getString("chargetypecode"));
                charge.put("name", rs.getString("name"));
                charge.put("value", rs.getBigDecimal("value"));
                charge.put("status", rs.getString("status"));
                charges.add(charge);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching charges for " + itemCode, e);
        }
        return charges;
    }

    public Map<String, Object> debugAttributeColumns() {
        Map<String, Object> result = new HashMap<>();
        String sql = "SELECT * FROM ecm.cwpc_itemattribute_v WHERE projectcode = ? FETCH FIRST 5 ROWS ONLY";
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnName(i) + " (" + meta.getColumnTypeName(i) + ")");
            }
            result.put("columns", columns);

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getString(i));
                }
                rows.add(row);
            }
            result.put("sampleRows", rows);
        } catch (SQLException e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}
