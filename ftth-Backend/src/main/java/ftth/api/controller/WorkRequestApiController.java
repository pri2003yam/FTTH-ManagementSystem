package ftth.api.controller;

import ftth.model.Notification;
import ftth.model.WorkRequest;
import ftth.model.enums.WorkRequestStatus;
import ftth.repository.NotificationRepository;
import ftth.repository.WorkRequestRepository;
import ftth.service.WorkRequestService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/work-requests")
public class WorkRequestApiController {

    private final WorkRequestService service;

    public WorkRequestApiController() {
        this.service = new WorkRequestService(new WorkRequestRepository(), new NotificationRepository());
    }

    // ─── Create ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        try {
            String pincode = body.get("pincode");
            String oltType = body.get("oltType");
            String actionType = body.getOrDefault("actionType", "ADD_OLT");
            String raisedBy = body.get("raisedBy");
            String description = body.get("description");

            WorkRequest wr = service.create(pincode, oltType, actionType, raisedBy, description);

            Map<String, Object> res = new HashMap<>();
            res.put("message", "Work request created successfully.");
            res.put("wrId", wr.getWrId());
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Transition ───────────────────────────────────────────

    @PostMapping("/{wrId}/transition")
    public ResponseEntity<Map<String, Object>> transition(
            @PathVariable long wrId,
            @RequestBody Map<String, String> body) {
        try {
            WorkRequestStatus newStatus = WorkRequestStatus.valueOf(body.get("newStatus"));
            String actor = body.get("actor");

            WorkRequest wr = service.transition(wrId, newStatus, actor);

            Map<String, Object> res = new HashMap<>();
            res.put("message", "Status updated to " + wr.getStatus());
            res.put("workRequest", toMap(wr));
            return ResponseEntity.ok(res);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── List ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(@RequestParam(value = "raisedBy", required = false) String raisedBy) {
        List<WorkRequest> list = (raisedBy != null && !raisedBy.isEmpty())
                ? service.getByUser(raisedBy)
                : service.getAll();
        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    @GetMapping("/open")
    public ResponseEntity<List<Map<String, Object>>> getOpen() {
        return ResponseEntity.ok(service.getOpenRequests().stream().map(this::toMap).toList());
    }

    @GetMapping("/{wrId}")
    public ResponseEntity<?> getById(@PathVariable long wrId) {
        WorkRequest wr = service.getById(wrId);
        if (wr == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMap(wr));
    }

    // ─── Notifications ────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getNotifications(@RequestParam("username") String username) {
        List<Notification> notifs = service.getNotifications(username);
        int unread = service.getUnreadCount(username);

        List<Map<String, Object>> items = notifs.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("notificationId", n.getNotificationId());
            m.put("wrId", n.getWrId());
            m.put("message", n.getMessage());
            m.put("isRead", n.isRead());
            m.put("createdAt", n.getCreatedAt().toString());
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("unreadCount", unread);
        res.put("notifications", items);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable long notificationId) {
        service.markNotificationRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Marked as read."));
    }

    // ─── Helper ───────────────────────────────────────────────

    private Map<String, Object> toMap(WorkRequest wr) {
        Map<String, Object> m = new HashMap<>();
        m.put("wrId", wr.getWrId());
        m.put("pincode", wr.getPincode());
        m.put("oltType", wr.getOltType());
        m.put("actionType", wr.getActionType());
        m.put("status", wr.getStatus().name());
        m.put("raisedBy", wr.getRaisedBy());
        m.put("assignedTo", wr.getAssignedTo());
        m.put("description", wr.getDescription());
        m.put("createdAt", wr.getCreatedAt().toString());
        m.put("updatedAt", wr.getUpdatedAt().toString());
        return m;
    }
}
