package ftth.model;

import ftth.model.enums.WorkRequestStatus;
import java.time.LocalDateTime;

public class WorkRequest {

    private Long wrId;
    private String pincode;
    private String oltType;
    private String actionType;
    private WorkRequestStatus status;
    private String raisedBy;
    private String assignedTo;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkRequest() {}

    public WorkRequest(String pincode, String oltType, String actionType, String raisedBy, String description) {
        this.pincode = pincode;
        this.oltType = oltType;
        this.actionType = actionType;
        this.status = WorkRequestStatus.NEW;
        this.raisedBy = raisedBy;
        this.description = description;
    }

    public Long getWrId() { return wrId; }
    public void setWrId(Long wrId) { this.wrId = wrId; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getOltType() { return oltType; }
    public void setOltType(String oltType) { this.oltType = oltType; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public WorkRequestStatus getStatus() { return status; }
    public void setStatus(WorkRequestStatus status) { this.status = status; }

    public String getRaisedBy() { return raisedBy; }
    public void setRaisedBy(String raisedBy) { this.raisedBy = raisedBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
