-- Remove account_type column since it's no longer used

ALTER TABLE accounts DROP COLUMN IF EXISTS account_type;
