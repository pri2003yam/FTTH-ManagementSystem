-- Add action_type to work_requests table
ALTER TABLE work_requests
ADD COLUMN action_type VARCHAR(30) NOT NULL DEFAULT 'ADD_OLT'
AFTER olt_type;
