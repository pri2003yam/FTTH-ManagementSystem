package ftth.api.controller;

import ftth.model.Plan;
import ftth.model.User;
import ftth.repository.EmailLogRepository;
import ftth.repository.PlanRepository;
import ftth.service.EmailService;
import ftth.service.PlanService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/plans")
@CrossOrigin(origins = "*")
public class PlanAdminApiController {

    private final PlanService planService;

    public PlanAdminApiController() {
        PlanRepository planRepo = new PlanRepository();
        EmailService emailService = new EmailService(new EmailLogRepository());
        this.planService = new PlanService(planRepo, emailService);
    }

    // GET /api/admin/plans
    // All plans with customer count
    @GetMapping
    public List<Map<String, Object>> getAllPlans() {
        List<Plan> plans = planService.getPlansWithCustomerCount();
        return plans.stream().map(this::toMap).toList();
    }

    // POST /api/admin/plans
    // Add new plan
    // Body: { planName, speedLabel, dataLimitLabel, ottCount, monthlyPrice, oltType }
    @PostMapping
    public ResponseEntity<Map<String, Object>> addPlan(@RequestBody Plan req) {
        if (req.getPlanName() == null || req.getPlanName().trim().length() < 2)
            return bad("planName is required (min 2 chars)");
        if (req.getSpeedLabel() == null || req.getSpeedLabel().trim().isEmpty())
            return bad("speedLabel is required");
        if (req.getDataLimitLabel() == null || req.getDataLimitLabel().trim().isEmpty())
            return bad("dataLimitLabel is required");
        if (req.getMonthlyPrice() == null || req.getMonthlyPrice().doubleValue() <= 0)
            return bad("monthlyPrice must be greater than 0");
        if (req.getOltType() == null || req.getOltType().trim().isEmpty())
            return bad("oltType is required");

        Plan plan = new Plan(
            req.getPlanName().trim(),
            req.getSpeedLabel().trim(),
            req.getDataLimitLabel().trim(),
            req.getOttCount(),
            req.getMonthlyPrice(),
            req.getOltType().toUpperCase().trim(),
            true
        );

        try {
            planService.addPlan(plan);
        } catch (RuntimeException e) {
            return bad(e.getMessage());
        }

        return ok("Plan added successfully");
    }

    // PUT /api/admin/plans/{id}
    // Update plan — oltType stays unchanged
    // Body: any fields you want to change
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePlan(
            @PathVariable long id,
            @RequestBody Plan req) {

        Plan existing = planService.findPlanById(id);
        if (existing == null)
            return bad("Plan not found with id=" + id);

        String planName       = notBlank(req.getPlanName())       ? req.getPlanName().trim()       : existing.getPlanName();
        String speedLabel     = notBlank(req.getSpeedLabel())     ? req.getSpeedLabel().trim()     : existing.getSpeedLabel();
        String dataLimitLabel = notBlank(req.getDataLimitLabel()) ? req.getDataLimitLabel().trim() : existing.getDataLimitLabel();
        int ottCount          = req.getOttCount() != null && req.getOttCount() >= 0
                                    ? req.getOttCount() : existing.getOttCount();
        java.math.BigDecimal price = req.getMonthlyPrice() != null && req.getMonthlyPrice().doubleValue() > 0
                                    ? req.getMonthlyPrice() : existing.getMonthlyPrice();

        Plan updated = new Plan(
            planName,
            speedLabel,
            dataLimitLabel,
            ottCount,
            price,
            existing.getOltType(),
            existing.isActive()
        );

        try {
            planService.updatePlan(id, updated, dummyUser());
        } catch (RuntimeException e) {
            return bad(e.getMessage());
        }

        return ok("Plan updated successfully");
    }

    // PATCH /api/admin/plans/{id}/toggle
    // Enable or Disable — allowed even if plan has customers
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> togglePlan(@PathVariable long id) {
        Plan plan = planService.findPlanById(id);
        if (plan == null)
            return bad("Plan not found with id=" + id);

        boolean newStatus = !plan.isActive();
        try {
            planService.togglePlanStatus(id, newStatus);
        } catch (RuntimeException e) {
            return bad(e.getMessage());
        }

        String newState = newStatus ? "Enabled" : "Disabled";
        return ok("Plan '" + plan.getPlanName() + "' is now " + newState);
    }

    // DELETE /api/admin/plans/{id}
    // Blocked if plan has active customers
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable long id) {
        Plan plan = planService.findPlanById(id);
        if (plan == null)
            return bad("Plan not found with id=" + id);

        try {
            boolean deleted = planService.deletePlan(id, dummyUser());
            if (!deleted)
                return bad("Cannot delete '" + plan.getPlanName() + "' — it has active customers.");
        } catch (RuntimeException e) {
            return bad(e.getMessage());
        }

        return ok("Plan '" + plan.getPlanName() + "' deleted successfully");
    }

    // ── Helpers ──

    private Map<String, Object> toMap(Plan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("planId",         p.getPlanId());
        m.put("planName",       p.getPlanName());
        m.put("speedLabel",     p.getSpeedLabel());
        m.put("dataLimitLabel", p.getDataLimitLabel());
        m.put("ottCount",       p.getOttCount());
        m.put("monthlyPrice",   p.getMonthlyPrice());
        m.put("oltType",        p.getOltType());
        m.put("active",         p.isActive());
        m.put("customerCount",  p.getCustomerCount());
        return m;
    }

    private ResponseEntity<Map<String, Object>> ok(String msg) {
        Map<String, Object> res = new HashMap<>();
        res.put("message", msg);
        return ResponseEntity.ok(res);
    }

    private ResponseEntity<Map<String, Object>> bad(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", msg);
        return ResponseEntity.badRequest().body(err);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private User dummyUser() {
        User u = new User();
        u.setUserId(1L);
        return u;
    }
}