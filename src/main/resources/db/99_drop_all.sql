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

-- Sequences are independent objects and are not removed with their tables.
DROP SEQUENCE yigit_seq_history;
DROP SEQUENCE yigit_seq_notifications;
DROP SEQUENCE yigit_seq_workflows;
DROP SEQUENCE yigit_seq_priorizations;
DROP SEQUENCE yigit_seq_requests;
DROP SEQUENCE yigit_seq_users;

--------------------------------------------------------------------------------
-- Verification: both queries should return no rows
--------------------------------------------------------------------------------
-- SELECT table_name    FROM user_tables    WHERE table_name    LIKE 'YIGIT!_%' ESCAPE '!';
-- SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'YIGIT!_%' ESCAPE '!';
