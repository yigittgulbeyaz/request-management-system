--------------------------------------------------------------------------------
-- Request Management System - Table Definitions
-- Target: Oracle Database 12.1.0.2
--
-- All objects carry the YIGIT_ prefix: the schema is shared between interns,
-- so unprefixed names such as USERS or REQUESTS would collide.
--
-- Primary keys are sequence-backed rather than identity columns. On this
-- Oracle version the current JDBC driver cannot return a generated key to
-- Hibernate after an insert, which fails with ORA-17023. A sequence is read
-- before the insert, so nothing has to be read back; it also leaves JDBC
-- batching available, which identity columns disable.
--
-- Execution order: 01 -> 02 -> (seed scripts)
-- Rollback: run 99_drop_all.sql
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- Sequences
--------------------------------------------------------------------------------
CREATE SEQUENCE yigit_seq_users         START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE yigit_seq_requests      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE yigit_seq_priorizations START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE yigit_seq_workflows     START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE yigit_seq_notifications START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE yigit_seq_history       START WITH 1 INCREMENT BY 1 NOCACHE;

-- INCREMENT BY 1 must match allocationSize on the entity's @SequenceGenerator.
-- A mismatch makes Hibernate skip identifiers.


--------------------------------------------------------------------------------
-- YIGIT_USERS
--------------------------------------------------------------------------------
CREATE TABLE yigit_users (
    user_id                 NUMBER              NOT NULL,
    name_surname            VARCHAR2(100 CHAR)  NOT NULL,
    email                   VARCHAR2(100 CHAR)  NOT NULL,
    password_hash           VARCHAR2(255 CHAR)  NOT NULL,
    role                    VARCHAR2(20 CHAR)   NOT NULL,
    is_active               NUMBER(1)           DEFAULT 1 NOT NULL,
    must_change_password    NUMBER(1)           DEFAULT 0 NOT NULL,
    failed_reset_attempts   NUMBER(1)           DEFAULT 0 NOT NULL,
    is_locked               NUMBER(1)           DEFAULT 0 NOT NULL,
    preferred_theme         VARCHAR2(10 CHAR)   DEFAULT 'light' NOT NULL,
    preferred_language      VARCHAR2(5 CHAR)    DEFAULT 'tr' NOT NULL,
    created_at              TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    security_question       VARCHAR2(100 CHAR),
    security_answer_hash    VARCHAR2(255 CHAR),
    setup_token             VARCHAR2(64 CHAR),
    setup_token_expires_at  TIMESTAMP,

    CONSTRAINT yigit_pk_users
        PRIMARY KEY (user_id),
    CONSTRAINT yigit_uk_users_email
        UNIQUE (email),
    CONSTRAINT yigit_ck_users_role
        CHECK (role IN ('CUSTOMER', 'PRODUCT_OWNER', 'DEVELOPER', 'ADMIN')),
    CONSTRAINT yigit_ck_users_active
        CHECK (is_active IN (0, 1)),
    CONSTRAINT yigit_ck_users_chg_pwd
        CHECK (must_change_password IN (0, 1)),
    CONSTRAINT yigit_ck_users_locked
        CHECK (is_locked IN (0, 1)),
    CONSTRAINT yigit_ck_users_attempts
        CHECK (failed_reset_attempts BETWEEN 0 AND 9),
    CONSTRAINT yigit_ck_users_theme
        CHECK (preferred_theme IN ('light', 'dark')),
    CONSTRAINT yigit_ck_users_language
        CHECK (preferred_language IN ('tr', 'en'))
);

COMMENT ON TABLE  yigit_users IS 'System users across all four roles';
COMMENT ON COLUMN yigit_users.is_active IS 'Soft delete flag - users are never physically removed';
COMMENT ON COLUMN yigit_users.is_locked IS 'Set when failed_reset_attempts exceeds the limit; distinct from is_active';
COMMENT ON COLUMN yigit_users.security_question IS 'Enum constant name, not the question text (kept translatable)';


--------------------------------------------------------------------------------
-- YIGIT_REQUESTS
--------------------------------------------------------------------------------
CREATE TABLE yigit_requests (
    request_id          NUMBER              NOT NULL,
    customer_id         NUMBER              NOT NULL,
    title               VARCHAR2(200 CHAR)  NOT NULL,
    description         CLOB                NOT NULL,
    status              VARCHAR2(30 CHAR)   DEFAULT 'NEW' NOT NULL,
    rejection_reason    VARCHAR2(500 CHAR),
    created_at          TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    closed_at           TIMESTAMP,

    CONSTRAINT yigit_pk_requests
        PRIMARY KEY (request_id),
    CONSTRAINT yigit_fk_requests_customer
        FOREIGN KEY (customer_id) REFERENCES yigit_users (user_id),
    CONSTRAINT yigit_ck_requests_status
        CHECK (status IN ('NEW', 'PRIORITIZED', 'IN_WORKFLOW', 'CLOSED', 'REJECTED')),
    CONSTRAINT yigit_ck_requests_title
        CHECK (LENGTH(TRIM(title)) >= 5),
    CONSTRAINT yigit_ck_requests_closed
        CHECK ((status = 'CLOSED' AND closed_at IS NOT NULL)
            OR (status <> 'CLOSED' AND closed_at IS NULL))
);

COMMENT ON TABLE  yigit_requests IS 'Customer-submitted requests';
COMMENT ON COLUMN yigit_requests.description IS 'CLOB - free text may exceed the 4000 byte VARCHAR2 limit';
COMMENT ON COLUMN yigit_requests.rejection_reason IS 'Mandatory at service level when status is REJECTED; shown to the customer';
COMMENT ON COLUMN yigit_requests.closed_at IS 'Set automatically when the linked workflow reaches DONE';


--------------------------------------------------------------------------------
-- YIGIT_PRIORIZATIONS
--------------------------------------------------------------------------------
CREATE TABLE yigit_priorizations (
    priority_id     NUMBER      NOT NULL,
    request_id      NUMBER      NOT NULL,
    impact          NUMBER(1)   NOT NULL,
    urgency         NUMBER(1)   NOT NULL,
    priority_score  NUMBER(2)   GENERATED ALWAYS AS (impact * urgency) VIRTUAL,
    prioritized_by  NUMBER      NOT NULL,
    created_at      TIMESTAMP   DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,

    CONSTRAINT yigit_pk_priorizations
        PRIMARY KEY (priority_id),
    CONSTRAINT yigit_uk_prio_request
        UNIQUE (request_id),
    CONSTRAINT yigit_fk_prio_request
        FOREIGN KEY (request_id) REFERENCES yigit_requests (request_id),
    CONSTRAINT yigit_fk_prio_user
        FOREIGN KEY (prioritized_by) REFERENCES yigit_users (user_id),
    CONSTRAINT yigit_ck_prio_impact
        CHECK (impact BETWEEN 1 AND 5),
    CONSTRAINT yigit_ck_prio_urgency
        CHECK (urgency BETWEEN 1 AND 5)
);

COMMENT ON TABLE  yigit_priorizations IS 'Impact/urgency scoring - absence of a row means the request is unscored';
COMMENT ON COLUMN yigit_priorizations.priority_score IS 'Virtual column - derived from impact * urgency, never supplied by the application';
COMMENT ON COLUMN yigit_priorizations.request_id IS 'UNIQUE - enforces the one-to-one relationship with requests';


--------------------------------------------------------------------------------
-- YIGIT_WORKFLOWS
--------------------------------------------------------------------------------
CREATE TABLE yigit_workflows (
    task_id         NUMBER              NOT NULL,
    request_id      NUMBER              NOT NULL,
    developer_id    NUMBER,
    workflow_status VARCHAR2(30 CHAR)   DEFAULT 'BACKLOG' NOT NULL,
    created_at      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    assigned_at     TIMESTAMP,
    deadline        TIMESTAMP,

    CONSTRAINT yigit_pk_workflows
        PRIMARY KEY (task_id),
    CONSTRAINT yigit_uk_wf_request
        UNIQUE (request_id),
    CONSTRAINT yigit_fk_wf_request
        FOREIGN KEY (request_id) REFERENCES yigit_requests (request_id),
    CONSTRAINT yigit_fk_wf_developer
        FOREIGN KEY (developer_id) REFERENCES yigit_users (user_id),
    CONSTRAINT yigit_ck_wf_status
        CHECK (workflow_status IN ('BACKLOG', 'IN_PROGRESS', 'TESTING', 'DONE')),
    CONSTRAINT yigit_ck_wf_assignment
        CHECK ((developer_id IS NULL     AND assigned_at IS NULL)
            OR (developer_id IS NOT NULL AND assigned_at IS NOT NULL)),
    CONSTRAINT yigit_ck_wf_unassigned
        CHECK (developer_id IS NOT NULL OR workflow_status = 'BACKLOG')
);

COMMENT ON TABLE  yigit_workflows IS 'Development tasks derived from prioritized requests';
COMMENT ON COLUMN yigit_workflows.developer_id IS 'Nullable - a task may sit unclaimed in BACKLOG';
COMMENT ON COLUMN yigit_workflows.assigned_at IS 'Supports cycle-time metrics; set together with developer_id';
COMMENT ON COLUMN yigit_workflows.deadline IS 'Committed at conversion, derived from the priority score. Stored rather than computed so a change to the formula does not move promises already made.';


--------------------------------------------------------------------------------
-- YIGIT_NOTIFICATIONS
--------------------------------------------------------------------------------
CREATE TABLE yigit_notifications (
    notification_id     NUMBER              NOT NULL,
    user_id             NUMBER              NOT NULL,
    message             VARCHAR2(255 CHAR)  NOT NULL,
    is_read             NUMBER(1)           DEFAULT 0 NOT NULL,
    related_request_id  NUMBER,
    created_at          TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT yigit_pk_notifications
        PRIMARY KEY (notification_id),
    CONSTRAINT yigit_fk_notif_user
        FOREIGN KEY (user_id) REFERENCES yigit_users (user_id),
    CONSTRAINT yigit_fk_notif_request
        FOREIGN KEY (related_request_id) REFERENCES yigit_requests (request_id),
    CONSTRAINT yigit_ck_notif_is_read
        CHECK (is_read IN (0, 1))
);

COMMENT ON TABLE yigit_notifications IS 'In-app notifications, written in the same transaction as the triggering state change';


--------------------------------------------------------------------------------
-- YIGIT_REQUEST_STATUS_HISTORY
--------------------------------------------------------------------------------
CREATE TABLE yigit_request_status_history (
    history_id  NUMBER              NOT NULL,
    request_id  NUMBER              NOT NULL,
    old_status  VARCHAR2(30 CHAR),
    new_status  VARCHAR2(30 CHAR)   NOT NULL,
    changed_by  NUMBER              NOT NULL,
    changed_at  TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT yigit_pk_history
        PRIMARY KEY (history_id),
    CONSTRAINT yigit_fk_hist_request
        FOREIGN KEY (request_id) REFERENCES yigit_requests (request_id),
    CONSTRAINT yigit_fk_hist_user
        FOREIGN KEY (changed_by) REFERENCES yigit_users (user_id),
    CONSTRAINT yigit_ck_hist_transition
        CHECK (old_status IS NULL OR old_status <> new_status)
);

COMMENT ON TABLE  yigit_request_status_history IS 'Append-only audit trail covering both request and workflow transitions';
COMMENT ON COLUMN yigit_request_status_history.old_status IS 'NULL for the initial creation event';
