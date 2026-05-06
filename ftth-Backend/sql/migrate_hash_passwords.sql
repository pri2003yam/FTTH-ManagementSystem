-- ============================================================
-- Migration: Hash all plain text passwords using SHA-256
-- Run this ONCE on any existing database
-- After running this, all passwords will be stored as SHA-256 hashes
-- ============================================================

USE testdb;

-- SHA2(password, 256) is MySQL's built-in SHA-256 function
-- This updates ALL users whose password is not already a 64-char hex hash

SET SQL_SAFE_UPDATES = 0;
UPDATE users
SET password_hash = SHA2(password_hash, 256)
WHERE LENGTH(password_hash) != 64;
SET SQL_SAFE_UPDATES = 1;
