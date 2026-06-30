CREATE TABLE IF NOT EXISTS port_reservations (
    reservation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    port_id BIGINT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    olt_type VARCHAR(20) NOT NULL,
    status ENUM('RESERVED','CONFIRMED','RELEASED','EXPIRED') NOT NULL DEFAULT 'RESERVED',
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    UNIQUE KEY uq_port_reserved (port_id, status)
);
