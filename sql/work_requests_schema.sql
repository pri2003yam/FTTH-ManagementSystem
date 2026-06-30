-- ============================================================
-- Work Request Module — Schema
-- Run AFTER main schema.sql (does NOT modify existing tables)
-- ============================================================

USE testdb;

-- ============================================================
-- 1. work_requests
-- ============================================================

CREATE TABLE IF NOT EXISTS work_requests (
    wr_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pincode         VARCHAR(10)   NOT NULL,
    olt_type        VARCHAR(20)   NOT NULL,
    status          ENUM('NEW','ACCEPTED','IN_PROGRESS','RESOLVED','CLOSED') NOT NULL DEFAULT 'NEW',
    raised_by       VARCHAR(50)   NOT NULL,
    assigned_to     VARCHAR(50)   DEFAULT NULL,
    description     VARCHAR(500)  DEFAULT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_wr_pincode_olt (pincode, olt_type, status)
);

-- NOTE: Duplicate prevention (one active WR per pincode+olt_type) is enforced
-- at the application layer in WorkRequestRepository.hasActiveRequest()

-- ============================================================
-- 2. notifications
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wr_id           BIGINT        NOT NULL,
    username        VARCHAR(50)   NOT NULL,
    message         VARCHAR(500)  NOT NULL,
    is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_notif_user (username, is_read),
    FOREIGN KEY (wr_id) REFERENCES work_requests(wr_id) ON DELETE CASCADE
);
