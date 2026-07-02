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

    // Find PS linked to a PO via product specification relation
    public String findPsForPo(String poItemCode) {
        String sql = "SELECT r.relateditemcode FROM ecm.cwpc_itemrelation r " +
                     "JOIN ecm.cwpc_item i ON i.itemcode = r.relateditemcode " +
                     "WHERE r.itemcode = ? AND i.itemtype = 'ProductSpecification' AND r.projectcode = ?";
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, poItemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("relateditemcode");
        } catch (SQLException e) {
            throw new RuntimeException("Error finding PS for PO: " + poItemCode, e);
        }
        return null;
    }

    // Find all CFSS linked to a PS
    public List<Map<String, Object>> findCfssForPs(String psItemCode) {
        String sql = "SELECT i.itemcode, i.name, i.description, i.status " +
                     "FROM ecm.cwpc_itemrelation r " +
                     "JOIN ecm.cwpc_item i ON i.itemcode = r.relateditemcode " +
                     "WHERE r.itemcode = ? AND i.itemtype = 'CustomerFacingServiceSpec' AND r.projectcode = ?";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, psItemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemCode", rs.getString("itemcode"));
                row.put("name", rs.getString("name"));
                row.put("description", rs.getString("description"));
                row.put("status", rs.getString("status"));
                row.put("attributes", findAttributes(rs.getString("itemcode")));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding CFSS for PS: " + psItemCode, e);
        }
        return results;
    }

    // Find RFSS linked to a CFSS
    public Map<String, Object> findRfssForCfss(String cfssItemCode) {
        String sql = "SELECT i.itemcode, i.name, i.description, i.status " +
                     "FROM ecm.cwpc_itemrelation r " +
                     "JOIN ecm.cwpc_item i ON i.itemcode = r.relateditemcode " +
                     "WHERE r.itemcode = ? AND i.itemtype = 'ResourceFacingServiceSpec' AND r.projectcode = ?";
        try (Connection conn = ecmDb.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cfssItemCode);
            ps.setString(2, projectCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemCode", rs.getString("itemcode"));
                row.put("name", rs.getString("name"));
                row.put("description", rs.getString("description"));
                row.put("status", rs.getString("status"));
                row.put("attributes", findAttributes(rs.getString("itemcode")));
                return row;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding RFSS for CFSS: " + cfssItemCode, e);
        }
        return null;
    }

    // Full decomposition: PO -> PS -> CFSS (with RFSS per CFSS)
    public Map<String, Object> decomposeOffering(String poItemCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("po", findProductOffering(poItemCode));

        String psCode = findPsForPo(poItemCode);
        if (psCode != null) {
            Map<String, Object> ps = new HashMap<>();
            ps.put("itemCode", psCode);
            ps.put("cfss", new ArrayList<>());

            List<Map<String, Object>> cfssList = findCfssForPs(psCode);
            for (Map<String, Object> cfss : cfssList) {
                Map<String, Object> rfss = findRfssForCfss((String) cfss.get("itemCode"));
                cfss.put("rfss", rfss);
                // Extract oltType from RFSS attributes for OLT auto-selection
                if (rfss != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> attrs = (List<Map<String, String>>) rfss.get("attributes");
                    if (attrs != null) {
                        attrs.stream()
                            .filter(a -> "oltType".equals(a.get("name")) || "oltType".equals(a.get("code")))
                            .findFirst()
                            .ifPresent(a -> result.put("oltType", a.get("value")));
                    }
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cfssBucket = (List<Map<String, Object>>) ps.get("cfss");
                cfssBucket.add(cfss);
            }
            result.put("ps", ps);
        }
        return result;
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
