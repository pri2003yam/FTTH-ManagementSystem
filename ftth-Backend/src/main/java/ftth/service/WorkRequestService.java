package ftth.service;

import ftth.model.Notification;
import ftth.model.WorkRequest;
import ftth.model.enums.WorkRequestStatus;
import ftth.repository.NotificationRepository;
import ftth.repository.WorkRequestRepository;

import java.util.List;

public class WorkRequestService {

    private final WorkRequestRepository wrRepo;
    private final NotificationRepository notifRepo;

    public WorkRequestService(WorkRequestRepository wrRepo, NotificationRepository notifRepo) {
        this.wrRepo = wrRepo;
        this.notifRepo = notifRepo;
    }

    public WorkRequest create(String pincode, String oltType, String actionType, String raisedBy, String description) {
        if (wrRepo.hasActiveRequest(pincode, oltType)) {
            throw new IllegalStateException("An active work request already exists for pincode " + pincode + " / " + oltType);
        }

        WorkRequest wr = new WorkRequest(pincode, oltType, actionType, raisedBy, description);
        long id = wrRepo.insert(wr);
        wr.setWrId(id);

        notifyAllMaintenance(id, "New work request #" + id + " raised for " + pincode + " (" + oltType + ") — Action: " + actionType);
        return wr;
    }

    public WorkRequest transition(long wrId, WorkRequestStatus newStatus, String actor) {
        WorkRequest wr = wrRepo.findById(wrId);
        if (wr == null) throw new IllegalArgumentException("Work request not found: " + wrId);

        validateTransition(wr.getStatus(), newStatus);

        String assignedTo = wr.getAssignedTo();

        switch (newStatus) {
            case ACCEPTED:
                assignedTo = actor;
                wrRepo.updateStatus(wrId, newStatus, assignedTo);
                // Clear notifications for this WR from all maintenance users
                notifRepo.deleteByWrId(wrId);
                break;

            case NEW:
                // Release back to queue
                assignedTo = null;
                wrRepo.updateStatus(wrId, newStatus, null);
                notifyAllMaintenance(wrId, "Work request #" + wrId + " released back to queue (" + wr.getPincode() + " / " + wr.getOltType() + ")");
                break;

            case CLOSED:
                wrRepo.updateStatus(wrId, newStatus, assignedTo);
                // Notify the CSR who raised it
                notifRepo.insert(new Notification(wrId, wr.getRaisedBy(), "Work request #" + wrId + " has been CLOSED."));
                break;

            default:
                wrRepo.updateStatus(wrId, newStatus, assignedTo);
                break;
        }

        return wrRepo.findById(wrId);
    }

    public List<WorkRequest> getAll() {
        return wrRepo.findAll();
    }

    public List<WorkRequest> getByUser(String username) {
        return wrRepo.findByRaisedBy(username);
    }

    public List<WorkRequest> getOpenRequests() {
        return wrRepo.findOpenRequests();
    }

    public WorkRequest getById(long wrId) {
        return wrRepo.findById(wrId);
    }

    public List<Notification> getNotifications(String username) {
        return notifRepo.findByUsername(username);
    }

    public int getUnreadCount(String username) {
        return notifRepo.countUnread(username);
    }

    public boolean markNotificationRead(long notificationId) {
        return notifRepo.markAsRead(notificationId);
    }

    private void notifyAllMaintenance(long wrId, String message) {
        List<String> maintUsers = notifRepo.findMaintenanceUsernames();
        for (String user : maintUsers) {
            notifRepo.insert(new Notification(wrId, user, message));
        }
    }

    private void validateTransition(WorkRequestStatus current, WorkRequestStatus target) {
        boolean valid = switch (current) {
            case NEW -> target == WorkRequestStatus.ACCEPTED || target == WorkRequestStatus.CLOSED;
            case ACCEPTED -> target == WorkRequestStatus.IN_PROGRESS || target == WorkRequestStatus.NEW;
            case IN_PROGRESS -> target == WorkRequestStatus.RESOLVED;
            case RESOLVED -> target == WorkRequestStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Invalid transition: " + current + " → " + target);
        }
    }
}
