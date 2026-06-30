package com.aaha.ftth.reservation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reserve")
public class ReservationController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${reservation.ttl-minutes}")
    private int ttlMinutes;

    public ReservationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * POST /api/reserve
     * Reserve an available port for the given pincode and OLT type.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> reservePort(@RequestBody Map<String, String> request) {
        String pincode = request.get("pincode");
        String oltType = request.get("oltType");

        // 1. Check service area active
        List<Map<String, Object>> serviceAreas = jdbcTemplate.queryForList(
                "SELECT service_area_id, is_active FROM service_areas WHERE pincode = ?", pincode);

        if (serviceAreas.isEmpty() || !Boolean.TRUE.equals(serviceAreas.get(0).get("is_active"))) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("reason", "Service not available in this pincode");
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Find available port (not already reserved)
        String findPortSql = """
                SELECT p.port_id FROM ports p
                JOIN splitters s ON s.splitter_id = p.splitter_id
                JOIN olts o ON o.olt_id = s.olt_id
                JOIN service_areas sa ON sa.service_area_id = o.service_area_id
                WHERE sa.pincode = ? AND o.olt_type = ? AND p.port_status = 'AVAILABLE'
                AND p.port_id NOT IN (SELECT port_id FROM port_reservations WHERE status = 'RESERVED' AND expires_at > NOW())
                LIMIT 1
                """;

        List<Map<String, Object>> ports = jdbcTemplate.queryForList(findPortSql, pincode, oltType);

        if (ports.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("reason", "No available ports for " + oltType + " in pincode " + pincode);
            return ResponseEntity.ok(response);
        }

        Long portId = ((Number) ports.get(0).get("port_id")).longValue();

        // 3. Insert reservation
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO port_reservations (port_id, pincode, olt_type, status, reserved_at, expires_at) " +
                            "VALUES (?, ?, ?, 'RESERVED', NOW(), DATE_ADD(NOW(), INTERVAL ? MINUTE))",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, portId);
            ps.setString(2, pincode);
            ps.setString(3, oltType);
            ps.setInt(4, ttlMinutes);
            return ps;
        }, keyHolder);

        Long reservationId = keyHolder.getKey().longValue();

        // Fetch the expires_at value
        Map<String, Object> reservation = jdbcTemplate.queryForMap(
                "SELECT expires_at FROM port_reservations WHERE reservation_id = ?", reservationId);
        Timestamp expiresAt = (Timestamp) reservation.get("expires_at");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "RESERVED");
        response.put("reservationId", reservationId);
        response.put("portId", portId);
        response.put("pincode", pincode);
        response.put("oltType", oltType);
        response.put("expiresAt", expiresAt.toLocalDateTime().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/reserve/{reservationId}/confirm
     * Confirm a reservation before it expires.
     */
    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmReservation(@PathVariable Long reservationId) {
        // 1. Check reservation exists and is valid
        List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT reservation_id, port_id, status, expires_at FROM port_reservations " +
                        "WHERE reservation_id = ? AND status = 'RESERVED' AND expires_at > NOW()",
                reservationId);

        if (reservations.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("reason", "Reservation expired or not found");
            return ResponseEntity.badRequest().body(response);
        }

        Long portId = ((Number) reservations.get(0).get("port_id")).longValue();

        // 2. Update reservation status
        jdbcTemplate.update(
                "UPDATE port_reservations SET status = 'CONFIRMED', confirmed_at = NOW() WHERE reservation_id = ?",
                reservationId);

        // 3. Update port status
        jdbcTemplate.update("UPDATE ports SET port_status = 'ASSIGNED' WHERE port_id = ?", portId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "CONFIRMED");
        response.put("reservationId", reservationId);
        response.put("portId", portId);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/reserve/{reservationId}/release
     * Release a reservation manually.
     */
    @PostMapping("/{reservationId}/release")
    public ResponseEntity<Map<String, Object>> releaseReservation(@PathVariable Long reservationId) {
        jdbcTemplate.update(
                "UPDATE port_reservations SET status = 'RELEASED', released_at = NOW() " +
                        "WHERE reservation_id = ? AND status = 'RESERVED'",
                reservationId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "RELEASED");
        response.put("reservationId", reservationId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/reserve/{reservationId}
     * Get reservation details.
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<Map<String, Object>> getReservation(@PathVariable Long reservationId) {
        List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT reservation_id, port_id, pincode, olt_type, status, reserved_at, expires_at " +
                        "FROM port_reservations WHERE reservation_id = ?",
                reservationId);

        if (reservations.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("reason", "Reservation not found");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> reservation = reservations.get(0);
        Map<String, Object> response = new HashMap<>();
        response.put("reservationId", reservation.get("reservation_id"));
        response.put("portId", reservation.get("port_id"));
        response.put("pincode", reservation.get("pincode"));
        response.put("oltType", reservation.get("olt_type"));
        response.put("status", reservation.get("status"));
        response.put("reservedAt", reservation.get("reserved_at") != null
                ? ((Timestamp) reservation.get("reserved_at")).toLocalDateTime().toString() : null);
        response.put("expiresAt", reservation.get("expires_at") != null
                ? ((Timestamp) reservation.get("expires_at")).toLocalDateTime().toString() : null);

        return ResponseEntity.ok(response);
    }
}
