package ftth.service;

import ftth.model.Plan;
import ftth.model.User;
import ftth.repository.PlanRepository;

import java.util.List;

public class PlanService {

    private final PlanRepository repo;
    private final EmailService emailService;

    public PlanService(PlanRepository repo,EmailService emailService) {
        this.repo = repo;
        this.emailService=emailService;
    }

public boolean addPlan(Plan plan) {
        return repo.insertPlan(plan);
    }
public void createPlan(Plan plan,User currUser) {

    boolean inserted = repo.insertPlan(plan);

    if (!inserted) {
        throw new RuntimeException("Plan creation failed");
    }
    emailService.sendPlanAdminEmail(
        "PLAN_ADDED",
        plan,
        currUser.getUserId()
    );
}

public List<Plan> getAllPlans() {
        return repo.findAllPlans();
    }
public List<Plan> getActivePlans() {
        return repo.findActivePlans();
    }
public void printActivePlans(List<Plan> plans) {

    if (plans.isEmpty()) {
        System.out.println("No active plans available.");
        return;
    }

  System.out.printf(
    "%-6s | %-15s | %-10s | %-18s | %-5s | %-10s | %-8s | %-10s%n",
    "ID", "Name", "Speed", "Data", "OTT", "Price", "OLT", "Customers"
);
System.out.println("=".repeat(100));

for (Plan p : plans) {
    System.out.printf(
        "%-6d | %-15s | %-10s | %-18s | %-5d | %-10.2f | %-8s | %-10d%n",
        p.getPlanId(),
        p.getPlanName(),
        p.getSpeedLabel(),
        p.getDataLimitLabel(),
        p.getOttCount(),
        p.getMonthlyPrice(),
        p.getOltType(),
        p.getCustomerCount()   // ✅ added customers column
    );
}
}
public void viewAllPlans() {
        List<Plan> plans = repo.findAllPlans();
        if (plans.isEmpty()) {
            System.out.println("No plans available.");
            return;
        }
        System.out.println("\n---- ALL PLANS ----");
        System.out.printf(
            "%-6s | %-15s | %-10s | %-18s | %-5s | %-10s | %-8s | %-8s%n",
            "ID", "Name", "Speed", "Data", "OTT", "Price", "OLT", "Status"
        );
        System.out.println("=".repeat(100));
        for (Plan p : plans) {
            System.out.printf(
                "%-6d | %-15s | %-10s | %-18s | %-5d | Rs.%-7s | %-8s | %-8s%n",
                p.getPlanId(),
                p.getPlanName(),
                p.getSpeedLabel(),
                p.getDataLimitLabel(),
                p.getOttCount(),
                p.getMonthlyPrice(),
                p.getOltType(),
                p.isActive() ? "ENABLED" : "DISABLED"
            );
        }
    }

public void updatePlan(long planId, Plan updatedPlan,User currUser) {

    Plan existingPlan = repo.findById(planId);

    if (existingPlan == null) {
        throw new RuntimeException("Plan not found with id=" + planId);
    }

    boolean updated = repo.updatePlan(planId, updatedPlan);

    if (!updated) {
        throw new RuntimeException("Plan update failed for id=" + planId);
    }
    emailService.sendPlanAdminEmail(
        "PLAN_UPDATED",
        updatedPlan,
        currUser.getUserId()
    );
}

public boolean togglePlanStatus(long id, boolean newStatus) {
        return repo.togglePlanStatus(id, newStatus);
    }

public boolean togglePlan(long id,User currUser) {
        Plan plan = repo.findPlanById(id);
        if (plan == null) return false;
        boolean newStatus = !plan.isActive();
        boolean toggled=repo.togglePlanStatus(id, newStatus);;
        if(toggled){emailService.sendPlanAdminEmail( newStatus ? "PLAN_ACTIVATED" : "PLAN_DEACTIVATED", plan, currUser.getUserId());}
        return toggled;
    }

public boolean deletePlan(long planId,User currUser) {
    Plan plan = repo.findById(planId);
    boolean deleted=repo.deletePlan(planId);
    if (deleted && plan != null) {
        emailService.sendPlanAdminEmail(
            "PLAN_DELETED",
            plan,
            currUser.getUserId()
        );
    }
        return deleted;
    }

public Plan getActivePlan(Long planId) {

        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        Plan plan = repo.findById(planId);

        if (plan == null) {
            throw new RuntimeException("Plan not found with ID: " + planId);
        }

        if (!plan.isActive()) {
            throw new RuntimeException(
                "Plan with ID " + planId + " is not active"
            );
        }

        return plan;
    }

public Plan findPlanById(Long planId) {
        return repo.findById(planId);
    }
    
    public List<Plan> getPlansWithCustomerCount() {
        return repo.findPlansWithCustomerCount();
    }

}






