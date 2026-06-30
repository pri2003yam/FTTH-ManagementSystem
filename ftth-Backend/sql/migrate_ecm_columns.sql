-- ============================================================
-- Migration: Add ECM Product Offering columns to customer_connections
-- Run this on the 'testdb' MySQL database
-- Safe to run multiple times — uses IF NOT EXISTS guards
-- ============================================================

-- Step 1: Make plan_id nullable (no longer required when using ECM offerings)
ALTER TABLE customer_connections MODIFY COLUMN plan_id BIGINT NULL;

-- Step 2: Add ECM offering columns (only if they don't exist yet)
SET @dbname = DATABASE();

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = 'customer_connections'
          AND COLUMN_NAME = 'ecm_item_code'
    ),
    'ALTER TABLE customer_connections ADD COLUMN ecm_item_code VARCHAR(100) NULL AFTER plan_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = 'customer_connections'
          AND COLUMN_NAME = 'offering_name'
    ),
    'ALTER TABLE customer_connections ADD COLUMN offering_name VARCHAR(200) NULL AFTER ecm_item_code',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = 'customer_connections'
          AND COLUMN_NAME = 'monthly_price'
    ),
    'ALTER TABLE customer_connections ADD COLUMN monthly_price DECIMAL(10,2) NULL AFTER offering_name',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = 'customer_connections'
          AND COLUMN_NAME = 'olt_type'
    ),
    'ALTER TABLE customer_connections ADD COLUMN olt_type VARCHAR(30) NULL AFTER monthly_price',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 3: Backfill existing connections from plans table
SET SQL_SAFE_UPDATES = 0;

UPDATE customer_connections cc
JOIN plans p ON p.plan_id = cc.plan_id
SET cc.offering_name = p.plan_name,
    cc.monthly_price = p.monthly_price,
    cc.olt_type      = p.olt_type
WHERE cc.offering_name IS NULL;

SET SQL_SAFE_UPDATES = 1;
