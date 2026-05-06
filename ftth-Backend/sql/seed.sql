-- ============================================================
-- Aaha Telecom FTTH — Seed Data
-- Run AFTER schema.sql
-- ============================================================

USE testdb;

-- ============================================================
-- 1. roles
-- ============================================================

INSERT INTO roles (role_code) VALUES ('ADMIN'), ('CSR'), ('MAINT');

-- ============================================================
-- 2. users
-- ============================================================

INSERT INTO users (username, password_hash, role_id, is_active) VALUES
('admin',    'admin123', 1, TRUE),
('csr1',     'csr123',   2, TRUE),
('maint1',   'maint123', 3, TRUE),
('testuser', 'test123',  2, TRUE);

-- ============================================================
-- 3. service_areas  (6 cities, 5-6 pincodes each)
-- ============================================================

INSERT INTO service_areas (pincode, is_active) VALUES
-- Bangalore
('560001', TRUE), ('560002', TRUE), ('560003', TRUE), ('560004', TRUE), ('560005', TRUE),
-- Hyderabad
('500001', TRUE), ('500002', TRUE), ('500003', TRUE), ('500004', TRUE), ('500005', TRUE), ('500006', TRUE),
-- Mumbai
('400001', TRUE), ('400002', TRUE), ('400003', TRUE), ('400004', TRUE), ('400005', TRUE),
-- Chennai
('600001', TRUE), ('600002', TRUE), ('600003', TRUE), ('600004', TRUE), ('600005', TRUE), ('600006', TRUE),
-- Pune
('411001', TRUE), ('411002', TRUE), ('411003', TRUE), ('411004', TRUE), ('411005', TRUE),
-- Delhi
('110001', TRUE), ('110002', TRUE), ('110003', TRUE), ('110004', TRUE), ('110005', TRUE), ('110006', TRUE);

-- ============================================================
-- 4. olts  (one OLT300 + one OLT500 per pincode)
-- ============================================================

INSERT INTO olts (olt_code, service_area_id, olt_type, is_active)
SELECT CONCAT('OLT300-', pincode, '-1'), service_area_id, 'OLT300', TRUE FROM service_areas;

INSERT INTO olts (olt_code, service_area_id, olt_type, is_active)
SELECT CONCAT('OLT500-', pincode, '-1'), service_area_id, 'OLT500', TRUE FROM service_areas;

-- ============================================================
-- 5. splitters  (2 per OLT300, 3 per OLT500)
-- ============================================================

INSERT INTO splitters (olt_id, splitter_code, splitter_number, is_active)
SELECT olt_id, CONCAT(olt_code, '-SPL1'), 1, TRUE FROM olts;

INSERT INTO splitters (olt_id, splitter_code, splitter_number, is_active)
SELECT olt_id, CONCAT(olt_code, '-SPL2'), 2, TRUE FROM olts;

INSERT INTO splitters (olt_id, splitter_code, splitter_number, is_active)
SELECT olt_id, CONCAT(olt_code, '-SPL3'), 3, TRUE FROM olts WHERE olt_type = 'OLT500';

-- ============================================================
-- 6. ports  (3 per splitter)
-- ============================================================

INSERT INTO ports (splitter_id, port_number, port_status)
SELECT splitter_id, 1, 'AVAILABLE' FROM splitters;

INSERT INTO ports (splitter_id, port_number, port_status)
SELECT splitter_id, 2, 'AVAILABLE' FROM splitters;

INSERT INTO ports (splitter_id, port_number, port_status)
SELECT splitter_id, 3, 'AVAILABLE' FROM splitters;

-- ============================================================
-- 7. plans
-- ============================================================

INSERT INTO plans (plan_code, plan_name, speed_label, data_limit_label, ott_count, monthly_price, olt_type, is_active) VALUES
('BASIC',     'Basic',     '100MBPS',  '30GB/Month',         1,  299.00, 'OLT300', TRUE),
('STARTER',   'Starter',   '150MBPS',  '50GB/Month',         1,  399.00, 'OLT300', TRUE),
('SILVER',    'Silver',    '300MBPS',  '60GB/Month',         2,  499.00, 'OLT300', TRUE),
('BRONZE',    'Bronze',    '200MBPS',  '100GB/Month',        2,  599.00, 'OLT300', TRUE),
('STANDARD',  'Standard',  '300MBPS',  '150GB/Month',        3,  799.00, 'OLT300', TRUE),
('GOLD',      'Gold',      '500MBPS',  'Unlimited Internet', 4,  999.00, 'OLT500', TRUE),
('PREMIUM',   'Premium',   '600MBPS',  'Unlimited Internet', 5, 1199.00, 'OLT500', TRUE),
('ULTRA',     'Ultra',     '800MBPS',  'Unlimited Internet', 6, 1399.00, 'OLT500', TRUE),
('PLATINUM',  'Platinum',  '1000MBPS', 'Unlimited Internet', 6, 1499.00, 'OLT500', TRUE),
('DIAMOND',   'Diamond',   '1200MBPS', 'Unlimited Internet', 8, 1799.00, 'OLT500', TRUE),
('ELITE',     'Elite',     '1500MBPS', 'Unlimited Internet',10, 1999.00, 'OLT500', TRUE);
INSERT INTO plans (
  plan_code, plan_name, speed_label, data_limit_label,
  ott_count, monthly_price, olt_type, is_active
) VALUES (
  'INFINITY',
  'Infinity',
  '2000MBPS',
  'Unlimited Internet',
  12,
  2499.00,
  'OLT500',
  TRUE
);

-- ============================================================
-- 8. customers
-- ============================================================

INSERT INTO customers (customer_code, full_name, email, salary, status) VALUES
('AAHA-0001', 'Ravi Kumar',     'ravi@aaha.demo',     45000.00, 'ACTIVE'),
('AAHA-0002', 'Priya Sharma',   'priya@aaha.demo',    52000.00, 'ACTIVE'),
('AAHA-0003', 'Shreya Rakesh',  'shreya@aaha.demo',   48000.00, 'ACTIVE'),
('AAHA-0004', 'Sai Rohith',     'rohith@aaha.demo',   84000.00, 'ACTIVE'),
('AAHA-0005', 'Priyam Raj',     'priyam@aaha.demo',   83500.00, 'ACTIVE'),
('AAHA-0006', 'Manjunath R',    'manjunath@aaha.demo',187000.00,'ACTIVE'),
('AAHA-0007', 'Kiran Narayana', 'kiran@aaha.demo',   125000.00, 'ACTIVE'),
('AAHA-0008', 'Anvitha Reddy',  'anvitha@aaha.demo',  82500.00, 'ACTIVE'),
('AAHA-0009', 'Suresh P',       'suresh@aaha.demo',  196000.00, 'ACTIVE'),
('AAHA-0010', 'Gaurav Goyal',   'gaurav@aaha.demo',  115000.00, 'ACTIVE'),
('AAHA-0011', 'Rakesh Varma',   'rakesh@aaha.demo',   58000.00, 'ACTIVE'),
('AAHA-0012', 'Sneha Kapoor',   'sneha@aaha.demo',    62000.00, 'ACTIVE'),
('AAHA-0013', 'Arjun Mehta',    'arjun@aaha.demo',    57000.00, 'ACTIVE'),
('AAHA-0014', 'Aashish Jha',    'aashish@aaha.demo',  83000.00, 'ACTIVE'),
('AAHA-0015', 'Divya Nair',     'divya@aaha.demo',    54000.00, 'ACTIVE'),
('AAHA-0016', 'Vikram Shetty',  'vikram@aaha.demo',   61000.00, 'ACTIVE'),
('AAHA-0017', 'Meena Pillai',   'meena@aaha.demo',    55000.00, 'ACTIVE'),
('AAHA-0018', 'Rohit Desai',    'rohit@aaha.demo',    72000.00, 'ACTIVE'),
('AAHA-0019', 'Kavya Iyer',     'kavya@aaha.demo',    49000.00, 'ACTIVE');

-- ============================================================
-- 9. Mark ports ASSIGNED for connections below
-- ============================================================

-- 15 ports: OLT300 splitter-1 port-1 across first 15 pincodes
UPDATE ports p
JOIN splitters s ON s.splitter_id = p.splitter_id
JOIN olts o      ON o.olt_id      = s.olt_id
JOIN service_areas sa ON sa.service_area_id = o.service_area_id
SET p.port_status = 'ASSIGNED'
WHERE sa.pincode IN ('560001','560002','560003','560004','560005',
                     '500001','500002','500003','500004','500005',
                     '500006','400001','400002','400003','400004')
  AND o.olt_type        = 'OLT300'
  AND s.splitter_number = 1
  AND p.port_number     = 1;

-- 400003 OLT300: assign 4 more ports (spl1-p2, spl1-p3, spl2-p1, spl2-p2) → 5/6 = 83%
UPDATE ports p
JOIN splitters s ON s.splitter_id = p.splitter_id
JOIN olts o      ON o.olt_id      = s.olt_id
JOIN service_areas sa ON sa.service_area_id = o.service_area_id
SET p.port_status = 'ASSIGNED'
WHERE sa.pincode = '400003'
  AND o.olt_type = 'OLT300'
  AND (
    (s.splitter_number = 1 AND p.port_number IN (2, 3))
    OR
    (s.splitter_number = 2 AND p.port_number IN (1, 2))
  );

-- ============================================================
-- 10. customer_connections  (one active connection per customer)
-- ============================================================

-- Helper: map customer → pincode → plan
-- AAHA-0001  560001  SILVER
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-01-10', 10
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0001' AND pl.plan_code = 'SILVER' AND sa.pincode = '560001';

-- AAHA-0002  560002  GOLD
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-01-15', 10
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0002' AND pl.plan_code = 'GOLD' AND sa.pincode = '560002';

-- AAHA-0003  560003  BASIC
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-01-20', 5
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0003' AND pl.plan_code = 'BASIC' AND sa.pincode = '560003';

-- AAHA-0004  560004  PLATINUM
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-02-01', 1
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0004' AND pl.plan_code = 'PLATINUM' AND sa.pincode = '560004';

-- AAHA-0005  560005  STANDARD
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-02-05', 5
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0005' AND pl.plan_code = 'STANDARD' AND sa.pincode = '560005';

-- AAHA-0006  500001  ELITE
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-02-10', 10
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0006' AND pl.plan_code = 'ELITE' AND sa.pincode = '500001';

-- AAHA-0007  500002  DIAMOND
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-02-15', 15
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0007' AND pl.plan_code = 'DIAMOND' AND sa.pincode = '500002';

-- AAHA-0008  500003  BRONZE
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-03-01', 1
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0008' AND pl.plan_code = 'BRONZE' AND sa.pincode = '500003';

-- AAHA-0009  500004  ULTRA
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-03-05', 5
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0009' AND pl.plan_code = 'ULTRA' AND sa.pincode = '500004';

-- AAHA-0010  500005  PREMIUM
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-03-10', 10
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0010' AND pl.plan_code = 'PREMIUM' AND sa.pincode = '500005';

-- AAHA-0011  500006  STARTER
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-03-15', 15
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0011' AND pl.plan_code = 'STARTER' AND sa.pincode = '500006';

-- AAHA-0012  400001  SILVER
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-03-20', 20
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0012' AND pl.plan_code = 'SILVER' AND sa.pincode = '400001';

-- AAHA-0013  400002  GOLD
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-01', 1
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0013' AND pl.plan_code = 'GOLD' AND sa.pincode = '400002';

-- AAHA-0014  400003  PLATINUM
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-05', 5
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0014' AND pl.plan_code = 'PLATINUM' AND sa.pincode = '400003';

-- AAHA-0016  400003  SILVER  (spl1-p2)
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-12', 12
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 2
WHERE c.customer_code = 'AAHA-0016' AND pl.plan_code = 'SILVER' AND sa.pincode = '400003';

-- AAHA-0017  400003  BASIC  (spl1-p3)
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-14', 14
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 3
WHERE c.customer_code = 'AAHA-0017' AND pl.plan_code = 'BASIC' AND sa.pincode = '400003';

-- AAHA-0018  400003  BRONZE  (spl2-p1)
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-16', 16
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 2
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0018' AND pl.plan_code = 'BRONZE' AND sa.pincode = '400003';

-- AAHA-0019  400003  STARTER  (spl2-p2)
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-18', 18
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 2
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 2
WHERE c.customer_code = 'AAHA-0019' AND pl.plan_code = 'STARTER' AND sa.pincode = '400003';

-- AAHA-0015  400004  BASIC
INSERT INTO customer_connections (customer_id, plan_id, port_id, service_area_id, connection_status, activated_on, billing_day)
SELECT c.customer_id, pl.plan_id, p.port_id, sa.service_area_id, 'ACTIVE', '2025-04-10', 10
FROM customers c, plans pl, service_areas sa
JOIN olts o ON o.service_area_id = sa.service_area_id AND o.olt_type = 'OLT300'
JOIN splitters s ON s.olt_id = o.olt_id AND s.splitter_number = 1
JOIN ports p ON p.splitter_id = s.splitter_id AND p.port_number = 1
WHERE c.customer_code = 'AAHA-0015' AND pl.plan_code = 'BASIC' AND sa.pincode = '400004';
