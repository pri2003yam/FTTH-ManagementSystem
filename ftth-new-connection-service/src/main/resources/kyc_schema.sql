-- ============================================================
-- KYC Records Table — PAN Verification Data
-- This table simulates a government KYC database
-- Used by the New Connection workflow for customer verification
-- ============================================================

CREATE TABLE IF NOT EXISTS kyc_records (
    kyc_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pan_number VARCHAR(10) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    pan_status ENUM('ACTIVE', 'INACTIVE', 'SURRENDERED', 'DECEASED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SEED DATA
-- 10 Valid PAN records (will pass KYC)
-- 6 Error cases (will fail KYC for various reasons)
-- ============================================================

-- Clear existing data
DELETE FROM kyc_records;

-- ─── 10 VALID RECORDS (KYC will PASS) ─────────────────────
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('ABCPK1234A', 'Rajesh Kumar',       '1985-03-15', 'ACTIVE'),
('BDFPL5678B', 'Priya Sharma',       '1990-07-22', 'ACTIVE'),
('CGHPM9012C', 'Amit Singh',         '1988-11-05', 'ACTIVE'),
('DKLPN3456D', 'Sneha Patel',        '1992-01-30', 'ACTIVE'),
('EMNPO7890E', 'Vikram Reddy',       '1987-09-18', 'ACTIVE'),
('FQRPS2345F', 'Anita Deshmukh',     '1995-04-12', 'ACTIVE'),
('GSTPQ6789G', 'Suresh Nair',        '1983-12-25', 'ACTIVE'),
('HUVPR1234H', 'Meera Iyer',         '1991-06-08', 'ACTIVE'),
('IWXPS5678I', 'Rohit Gupta',        '1986-08-20', 'ACTIVE'),
('JYZPT9012J', 'Kavita Joshi',       '1993-02-14', 'ACTIVE');

-- ─── 6 ERROR CASES (KYC will FAIL) ────────────────────────

-- Error 1: PAN is INACTIVE
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('XYZIN1111X', 'Inactive Person',    '1980-05-10', 'INACTIVE');

-- Error 2: PAN is SURRENDERED
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('SURPN2222S', 'Surrendered User',   '1975-08-20', 'SURRENDERED');

-- Error 3: PAN linked to DECEASED person
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('DECPN3333D', 'Deceased Person',    '1950-01-01', 'DECEASED');

-- Error 4: Name mismatch test — PAN exists but name won't match
-- (When someone sends name "Wrong Name" with this PAN, it will fail)
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('NMMPN4444N', 'Correct Full Name',  '1989-04-15', 'ACTIVE');

-- Error 5: DOB mismatch test — PAN exists but DOB won't match
-- (When someone sends wrong DOB with this PAN, it will fail)
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('DOBPN5555D', 'DOB Test User',      '1994-12-31', 'ACTIVE');

-- Error 6: Both name and DOB mismatch
INSERT INTO kyc_records (pan_number, full_name, date_of_birth, pan_status) VALUES
('BOTHP6666B', 'Both Mismatch Test', '1970-06-15', 'ACTIVE');
