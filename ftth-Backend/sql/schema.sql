-- ============================================================
-- Aaha Telecom FTTH — Full Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS testdb;
USE testdb;

-- ============================================================
-- Drop tables in reverse dependency order
-- ============================================================

DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS email_logs;
DROP TABLE IF EXISTS customer_connections;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS ports;
DROP TABLE IF EXISTS splitters;
DROP TABLE IF EXISTS olts;
DROP TABLE IF EXISTS service_areas;
DROP TABLE IF EXISTS plans;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

-- ============================================================
-- 1. roles
-- ============================================================

CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. users
-- ============================================================

CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ============================================================
-- 3. service_areas
-- ============================================================

CREATE TABLE service_areas (
    service_area_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pincode CHAR(6) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. olts
-- ============================================================

CREATE TABLE olts (
    olt_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    olt_code VARCHAR(50) NOT NULL UNIQUE,
    service_area_id BIGINT NOT NULL,
    olt_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_olts_service_area
        FOREIGN KEY (service_area_id)
        REFERENCES service_areas(service_area_id)
        ON DELETE CASCADE
);

-- ============================================================
-- 5. splitters
-- ============================================================

CREATE TABLE splitters (
    splitter_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    olt_id BIGINT NOT NULL,
    splitter_code VARCHAR(80) UNIQUE,
    splitter_number INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_splitters_olt
        FOREIGN KEY (olt_id)
        REFERENCES olts(olt_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_splitter_per_olt
        UNIQUE (olt_id, splitter_number)
);

-- ============================================================
-- 6. ports
-- ============================================================

CREATE TABLE ports (
    port_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    splitter_id BIGINT NOT NULL,
    port_number INT NOT NULL,
    port_status ENUM('AVAILABLE','ASSIGNED','FAULTY','DISABLED')
        NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ports_splitter
        FOREIGN KEY (splitter_id)
        REFERENCES splitters(splitter_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_port_per_splitter
        UNIQUE (splitter_id, port_number)
);

-- ============================================================
-- 7. plans
-- ============================================================

CREATE TABLE plans (
    plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    plan_name VARCHAR(100) NOT NULL,
    speed_label VARCHAR(40) NOT NULL,
    data_limit_label VARCHAR(100) NOT NULL,
    ott_count INT NOT NULL,
    monthly_price DECIMAL(10,2) NOT NULL,
    olt_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. customers
-- ============================================================

CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50) UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    salary DECIMAL(12,2) NOT NULL DEFAULT 0,
    status ENUM('ACTIVE','INACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 9. customer_connections
-- ============================================================

CREATE TABLE customer_connections (
    connection_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    port_id BIGINT NOT NULL,
    service_area_id BIGINT NOT NULL,
    connection_status ENUM('ACTIVE','DISCONNECTED') NOT NULL DEFAULT 'ACTIVE',
    activated_on DATE NOT NULL,
    disconnected_on DATE,
    billing_day TINYINT NOT NULL DEFAULT 10,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cc_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),
    CONSTRAINT fk_cc_plan
        FOREIGN KEY (plan_id)
        REFERENCES plans(plan_id),
    CONSTRAINT fk_cc_port
        FOREIGN KEY (port_id)
        REFERENCES ports(port_id),
    CONSTRAINT fk_cc_service_area
        FOREIGN KEY (service_area_id)
        REFERENCES service_areas(service_area_id),
    CONSTRAINT fk_cc_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(user_id),
    CONSTRAINT fk_cc_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(user_id)
);

-- ============================================================
-- 10. bills
-- ============================================================

CREATE TABLE bills (
    bill_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_no VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    plan_charge DECIMAL(10,2) NOT NULL,
    gst_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    bill_status ENUM('GENERATED','PAID','OVERDUE')
        NOT NULL DEFAULT 'GENERATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bills_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),
    CONSTRAINT fk_bills_connection
        FOREIGN KEY (connection_id)
        REFERENCES customer_connections(connection_id)
);

-- ============================================================
-- 11. email_logs
-- ============================================================

CREATE TABLE email_logs (
    email_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    email_type ENUM(
        'ORDER_CONFIRMATION',
        'BILL',
        'OLT_ALERT',
        'DISCONNECT'
    ) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    sent_status ENUM('SENT','FAILED') NOT NULL DEFAULT 'SENT',
    provider_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_logs_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
);
 
ALTER TABLE email_logs 
MODIFY COLUMN email_type ENUM(
    'ORDER_CONFIRMATION',
    'BILL',
    'OLT_ALERT',
    'DISCONNECT',
    'SERVICE_MOVE',
    'PLAN_CHANGE',
    'PLAN_ADMIN'
) NOT NULL;