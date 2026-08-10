--------------------------------------------------------------------------------
-- Request Management System - Drop All Objects
-- Target: Oracle Database 12.1.0.2
--
-- WARNING: destroys all data. Development use only.
--
-- The schema is shared between interns. Every statement below names a
-- YIGIT_ prefixed object - never remove the prefix from these lines.
--
-- Drop order follows foreign key dependencies in reverse: child tables first,
-- then parents. CASCADE CONSTRAINTS would make the order irrelevant, but
-- dropping in the correct order surfaces an unexpected dependency as an error
-- instead of silently removing it.
--------------------------------------------------------------------------------

DROP TABLE yigit_request_status_history;
DROP TABLE yigit_notifications;
DROP TABLE yigit_workflows;
DROP TABLE yigit_priorizations;
DROP TABLE yigit_requests;
DROP TABLE yigit_users;

-- Identity columns create sequences named ISEQ$$_<object_id>, which Oracle
-- drops automatically with the owning table. No manual sequence cleanup needed.

--------------------------------------------------------------------------------
-- Verification: should return no rows
--------------------------------------------------------------------------------
-- SELECT table_name FROM user_tables WHERE table_name LIKE 'YIGIT!_%' ESCAPE '!';
