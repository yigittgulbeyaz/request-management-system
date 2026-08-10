--------------------------------------------------------------------------------
-- Request Management System - Indexes
-- Target: Oracle Database 12.1.0.2
--
-- Primary keys and unique constraints are indexed automatically by Oracle;
-- only the additional indexes below are created explicitly.
--
-- Every index here is a prediction about a query pattern. The performance
-- tests planned for the list queries verify these predictions - an index that
-- never appears in an execution plan should be dropped rather than kept.
--------------------------------------------------------------------------------

-- "My Requests" filters on the session owner
CREATE INDEX yigit_idx_req_customer
    ON yigit_requests (customer_id);

-- PO pool filters by status; analytics group by it
CREATE INDEX yigit_idx_req_status
    ON yigit_requests (status);

-- Monthly volume report ranges over creation date
CREATE INDEX yigit_idx_req_created
    ON yigit_requests (created_at);

-- Developer task list filters on the assignee
CREATE INDEX yigit_idx_wf_developer
    ON yigit_workflows (developer_id);

-- Task board tabs filter by workflow stage
CREATE INDEX yigit_idx_wf_status
    ON yigit_workflows (workflow_status);

-- Unread badge queries both columns together.
-- Column order matters: user_id leads because every query filters on it,
-- while is_read only narrows further. Reversed, this index would be useless -
-- is_read has two distinct values and cannot narrow a scan on its own.
CREATE INDEX yigit_idx_notif_unread
    ON yigit_notifications (user_id, is_read);

-- Audit trail lookup per request, newest first
CREATE INDEX yigit_idx_hist_request
    ON yigit_request_status_history (request_id, changed_at DESC);

-- Test rework rate metric scans transitions by target status
CREATE INDEX yigit_idx_hist_new_status
    ON yigit_request_status_history (new_status);


--------------------------------------------------------------------------------
-- Deliberately not created
--------------------------------------------------------------------------------
-- yigit_requests.title / yigit_requests.description
--   Title search is a substring match, which a B-tree index cannot serve.
--   If search performance becomes a problem the answer is an Oracle Text
--   index, not a standard one - but at this system's scale it will not.
--
-- yigit_priorizations.priority_score
--   The PO pool sorts on this column, but the table holds at most one row per
--   request and the sort happens after a join already driven by the requests
--   table. Measure before adding.
--------------------------------------------------------------------------------
