package ftth.model;

import java.time.LocalDateTime;

public class Notification {

    private Long notificationId;
    private Long wrId;
    private String username;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(Long wrId, String username, String message) {
        this.wrId = wrId;
        this.username = username;
        this.message = message;
        this.read = false;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getWrId() { return wrId; }
    public void setWrId(Long wrId) { this.wrId = wrId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
