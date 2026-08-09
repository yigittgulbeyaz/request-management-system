# Entity Relationship Diagram

> Request Management System
> This document defines the database schema, the relationships between tables, and the reasoning behind each structural decision. It is the direct reference for writing the DDL scripts.

---

## 1. Diagram

```mermaid
erDiagram
    USERS ||--o{ REQUESTS : "submits"
    USERS ||--o{ PRIORIZATIONS : "scores"
    USERS ||--o{ WORKFLOWS : "is assigned"
    USERS ||--o{ NOTIFICATIONS : "receives"
    USERS ||--o{ REQUEST_STATUS_HISTORY : "performs change"

    REQUESTS ||--o| PRIORIZATIONS : "scored by exactly one"
    REQUESTS ||--o| WORKFLOWS : "converted into"
    REQUESTS ||--o{ NOTIFICATIONS : "referenced by"
    REQUESTS ||--o{ REQUEST_STATUS_HISTORY : "tracked by"

    USERS {
        number user_id PK
        varchar2 name_surname
        varchar2 email UK
        varchar2 password_hash
        varchar2 role
        number is_active
        number must_change_password
        varchar2 security_question
        varchar2 security_answer_hash
        number failed_reset_attempts
        number is_locked
        varchar2 preferred_theme
        varchar2 preferred_language
        timestamp created_at
    }

    REQUESTS {
        number request_id PK
        number customer_id FK
        varchar2 title
        clob description
        varchar2 status
        varchar2 rejection_reason
        timestamp created_at
        timestamp closed_at
    }

    PRIORIZATIONS {
        number priority_id PK
        number request_id FK_UK
        number impact
        number urgency
        number priority_score
        number prioritized_by FK
        timestamp created_at
        timestamp updated_at
    }

    WORKFLOWS {
        number task_id PK
        number request_id FK_UK
        number developer_id FK
        varchar2 workflow_status
        timestamp created_at
        timestamp assigned_at
    }

    NOTIFICATIONS {
        number notification_id PK
        number user_id FK
        varchar2 message
        number is_read
        number related_request_id FK
        timestamp created_at
    }

    REQUEST_STATUS_HISTORY {
        number history_id PK
        number request_id FK
        varchar2 old_status
        varchar2 new_status
        number changed_by FK
        timestamp changed_at
    }
```

---

## 2. Tables

### 2.1 `USERS`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `user_id` | `NUMBER` | PK, identity | Auto-generated |
| `name_surname` | `VARCHAR2(100)` | `NOT NULL` | |
| `email` | `VARCHAR2(100)` | `NOT NULL`, `UNIQUE` | Login identifier |
| `password_hash` | `VARCHAR2(255)` | `NOT NULL` | BCrypt output, ~60 chars; column sized for headroom |
| `role` | `VARCHAR2(20)` | `NOT NULL`, `CHECK` | `CUSTOMER`, `PRODUCT_OWNER`, `DEVELOPER`, `ADMIN` |
| `is_active` | `NUMBER(1)` | `NOT NULL`, default `1` | Soft delete flag |
| `must_change_password` | `NUMBER(1)` | `NOT NULL`, default `0` | Set when a temporary password is issued |
| `security_question` | `VARCHAR2(100)` | `NOT NULL` | Enum name, not the question text |
| `security_answer_hash` | `VARCHAR2(255)` | `NOT NULL` | Hashed with the same encoder as passwords |
| `failed_reset_attempts` | `NUMBER(1)` | `NOT NULL`, default `0` | Reset on success |
| `is_locked` | `NUMBER(1)` | `NOT NULL`, default `0` | Set when attempts exceed the limit |
| `preferred_theme` | `VARCHAR2(10)` | default `'light'` | |
| `preferred_language` | `VARCHAR2(5)` | default `'tr'` | |
| `created_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |

**`is_active` and `is_locked` are distinct states.** Inactive means an administrator disabled the account; locked means the self-service reset attempt limit was exceeded. They have different causes, different remedies, and different UI treatment, so collapsing them into one column would lose information.

**`security_question` stores the enum constant, not the question text.** Storing the text would break translation — the question must render in the user's chosen language, which is only possible if the stored value is a stable key.

### 2.2 `REQUESTS`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `request_id` | `NUMBER` | PK, identity | |
| `customer_id` | `NUMBER` | `NOT NULL`, FK → `USERS` | Always resolved from the session |
| `title` | `VARCHAR2(200)` | `NOT NULL` | |
| `description` | `CLOB` | `NOT NULL` | See note below |
| `status` | `VARCHAR2(30)` | `NOT NULL`, default `'NEW'`, `CHECK` | `NEW`, `PRIORITIZED`, `IN_WORKFLOW`, `CLOSED`, `REJECTED` |
| `rejection_reason` | `VARCHAR2(500)` | Nullable | Mandatory in the application when status is `REJECTED` |
| `created_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |
| `closed_at` | `TIMESTAMP` | Nullable | Set when status becomes `CLOSED` |

**`CLOB` rather than `VARCHAR2` for description.** Oracle's `VARCHAR2` caps at 4000 bytes by default, and a free-text problem description can plausibly exceed that. `CLOB` removes the ceiling. The trade-off is that `CLOB` columns cannot be indexed conventionally and are fetched separately — which is exactly why the list projections in §5 exclude the description entirely.

**`rejection_reason` is nullable at the database level** but mandatory when a request is rejected. Enforcing it as a table-level `CHECK` conditional on status is possible but couples the constraint to state-machine knowledge that already lives in the application. The rule is enforced in the service layer, where the rejection transition is handled.

**No `updated_at` column.** The status history table records every meaningful change with a timestamp, making a generic modification timestamp redundant.

### 2.3 `PRIORIZATIONS`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `priority_id` | `NUMBER` | PK, identity | |
| `request_id` | `NUMBER` | `NOT NULL`, FK → `REQUESTS`, `UNIQUE` | Enforces one-to-one |
| `impact` | `NUMBER(1)` | `NOT NULL`, `CHECK BETWEEN 1 AND 5` | |
| `urgency` | `NUMBER(1)` | `NOT NULL`, `CHECK BETWEEN 1 AND 5` | |
| `priority_score` | `NUMBER(2)` | Computed | `impact * urgency`, range 1–25 |
| `prioritized_by` | `NUMBER` | `NOT NULL`, FK → `USERS` | Which PO scored it |
| `created_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | Nullable | Set when a score is edited |

**The `UNIQUE` constraint on `request_id` is what makes this a one-to-one relationship.** Without it the schema would permit several competing scores for one request. It also removes the need for a separate "is scored" flag on `REQUESTS`: the absence of a row here *is* the unscored state, which is what the PO pool renders as "Not Assigned".

**`priority_score` is derived, never supplied by the application.** Two implementation options exist in Oracle:

| Approach | Mechanism | Assessment |
|---|---|---|
| Virtual column | `GENERATED ALWAYS AS (impact * urgency) VIRTUAL` | Preferred — declarative, no procedural code, cannot drift, computed on read |
| Trigger | `BEFORE INSERT OR UPDATE` assigning the value | Fallback if the target Oracle version predates virtual columns |

Either way the application sends only `impact` and `urgency`. A score calculated in Java and sent to the database would be a second source of truth and could disagree with it.

**Editing preserves the row.** When a PO revises a score the existing row is updated rather than replaced, so `prioritized_by` and `created_at` continue to describe the original scoring event while `updated_at` records the revision.

### 2.4 `WORKFLOWS`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `task_id` | `NUMBER` | PK, identity | |
| `request_id` | `NUMBER` | `NOT NULL`, FK → `REQUESTS`, `UNIQUE` | One workflow per request |
| `developer_id` | `NUMBER` | Nullable, FK → `USERS` | Null while unclaimed |
| `workflow_status` | `VARCHAR2(30)` | `NOT NULL`, default `'BACKLOG'`, `CHECK` | `BACKLOG`, `IN_PROGRESS`, `TESTING`, `DONE` |
| `created_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |
| `assigned_at` | `TIMESTAMP` | Nullable | Set when a developer is assigned or claims the task |

**`request_id` is `UNIQUE` here too.** The original specification did not mark it so, but the state machine requires it: a request transitions to `IN_WORKFLOW` exactly once, and a second workflow row for the same request would leave the automatic `DONE` → `CLOSED` transition ambiguous about which workflow closes it. Making the constraint explicit lets the database enforce what the state machine already assumes.

**`developer_id` is deliberately nullable.** A task can sit in `BACKLOG` unassigned — this is what makes the developer self-assign flow possible. The application enforces that a task cannot enter `IN_PROGRESS` while this column is null.

**`assigned_at` supports the cycle-time metrics.** Without it, "how long did this task wait in backlog" cannot be answered.

### 2.5 `NOTIFICATIONS`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `notification_id` | `NUMBER` | PK, identity | |
| `user_id` | `NUMBER` | `NOT NULL`, FK → `USERS` | Recipient |
| `message` | `VARCHAR2(255)` | `NOT NULL` | |
| `is_read` | `NUMBER(1)` | `NOT NULL`, default `0` | |
| `related_request_id` | `NUMBER` | Nullable, FK → `REQUESTS` | Enables click-through to the request |
| `created_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |

Notification rows are written inside the same transaction as the state change that triggers them. If the state change rolls back, the notification disappears with it — a user is never told about something that did not happen.

### 2.6 `REQUEST_STATUS_HISTORY`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `history_id` | `NUMBER` | PK, identity | |
| `request_id` | `NUMBER` | `NOT NULL`, FK → `REQUESTS` | |
| `old_status` | `VARCHAR2(30)` | Nullable | Null for the creation event |
| `new_status` | `VARCHAR2(30)` | `NOT NULL` | |
| `changed_by` | `NUMBER` | `NOT NULL`, FK → `USERS` | |
| `changed_at` | `TIMESTAMP` | `NOT NULL`, default `SYSTIMESTAMP` | |

**This table records both state machines.** Request-level transitions (`NEW` → `PRIORITIZED`) and workflow-level transitions (`IN_PROGRESS` → `TESTING`) are both written here, because the analytics need them together — the test rework rate counts `TESTING` → `IN_PROGRESS` rows, while resolution time spans the request-level lifecycle. Splitting them into two history tables would double the schema for no analytical gain.

**Append-only.** Rows are never updated or deleted. An audit trail that can be modified is not an audit trail.

---

## 3. Relationships

| Relationship | Cardinality | Enforcement |
|---|---|---|
| User → Requests | 1 : N | FK `requests.customer_id` |
| Request → Prioritization | 1 : 0..1 | FK + `UNIQUE` on `priorizations.request_id` |
| User (PO) → Prioritizations | 1 : N | FK `priorizations.prioritized_by` |
| Request → Workflow | 1 : 0..1 | FK + `UNIQUE` on `workflows.request_id` |
| User (Developer) → Workflows | 1 : N | FK `workflows.developer_id`, nullable |
| User → Notifications | 1 : N | FK `notifications.user_id` |
| Request → Notifications | 1 : N | FK `notifications.related_request_id`, nullable |
| Request → Status history | 1 : N | FK `request_status_history.request_id` |
| User → Status history | 1 : N | FK `request_status_history.changed_by` |

### 3.1 No Cascading Deletes

Every foreign key is declared without `ON DELETE CASCADE`.

Users are never deleted — deactivation sets `is_active = 0`, preserving the references in `workflows.developer_id`, `priorizations.prioritized_by`, and `request_status_history.changed_by`. A cascade here would erase the history of work performed by a departed employee.

Requests are likewise never deleted. A request that should not have been submitted is rejected, not removed, so the record of it remains.

The absence of cascades is therefore not an oversight: deletion is not part of the system's vocabulary, and a cascade rule would only take effect in a situation that should never arise.

### 3.2 Role Is Not Modelled as a Table

`users.role` is a constrained `VARCHAR2` mapped to a Java enum, not a foreign key to a `ROLES` table.

A reference table would be warranted if roles were user-defined, if permissions were configurable at runtime, or if a user could hold several roles simultaneously. None of these hold: the four roles are fixed by the application's design, their permissions are expressed in code through `@PreAuthorize`, and each user holds exactly one. A join table would add a query hop and a migration burden to model something the application already knows at compile time.

The same reasoning applies to `requests.status`, `workflows.workflow_status`, and `users.security_question`: all are enum-backed `VARCHAR2` columns with `CHECK` constraints.

The `CHECK` constraints matter here. They make the database reject a value the application would never produce, which catches a hand-written UPDATE during development or a bug in a data migration before it becomes corrupt state.

---

## 4. Indexes

Oracle creates indexes automatically for primary keys and unique constraints. The following are added explicitly:

| Index | Columns | Rationale |
|---|---|---|
| `IDX_REQUESTS_CUSTOMER` | `requests(customer_id)` | Every customer's "My Requests" query filters on this |
| `IDX_REQUESTS_STATUS` | `requests(status)` | PO pool filters by status; analytics group by it |
| `IDX_REQUESTS_CREATED` | `requests(created_at)` | Monthly volume report ranges over this |
| `IDX_WORKFLOWS_DEVELOPER` | `workflows(developer_id)` | Developer task list filters on this |
| `IDX_WORKFLOWS_STATUS` | `workflows(workflow_status)` | Task board tabs filter on this |
| `IDX_NOTIFICATIONS_UNREAD` | `notifications(user_id, is_read)` | Unread badge queries both columns together |
| `IDX_HISTORY_REQUEST` | `request_status_history(request_id)` | Audit trail lookup per request |

**Composite column order matters.** `IDX_NOTIFICATIONS_UNREAD` leads with `user_id` because every query filters on it, while `is_read` narrows further. Reversed, the index would be nearly useless — `is_read` has two distinct values and cannot narrow a scan on its own.

**Indexes not created:** none on `requests.title` or `requests.description`. Title search is a substring match, which a conventional B-tree index cannot serve. If search performance becomes a problem the answer is an Oracle Text index, not a standard one — but at this system's scale it will not.

**Verification, not assumption.** These are predictions about query patterns. The performance tests planned around the list queries measure whether they hold; an index that does not appear in an execution plan is dead weight and should be removed rather than kept for reassurance.

---

## 5. Query Projections

The PO pool screen needs, per row: request id, customer name, title, priority score, status. That data spans three tables.

Loading `Request` entities and navigating to `request.getCustomer().getName()` and `request.getPrioritization().getScore()` produces one query for the list plus two per row — the N+1 problem, and the reason the list query is defined as an explicit projection instead:

```
SELECT new RequestSummaryDto(r.id, u.nameSurname, r.title, p.priorityScore, r.status)
FROM Request r
JOIN r.customer u
LEFT JOIN r.prioritization p
```

One query, regardless of row count. `LEFT JOIN` is required because unscored requests must still appear — an inner join would silently hide exactly the rows the PO most needs to act on.

The projection also excludes `description`, keeping the `CLOB` out of a query that never displays it.

**No separate read-model repository.** The projection lives as a method on `RequestRepository` alongside the entity queries. A dedicated read repository would be justified if the read side used a different data source — a replica, a denormalised table, a search index. Reading the same tables through a different shape does not meet that bar; a second repository class would be a division without a distinction.

---

## 6. Version Dependency

Two schema decisions depend on the Oracle version of the target instance:

| Feature | Oracle 12c and later | Earlier versions |
|---|---|---|
| Primary key generation | `GENERATED ALWAYS AS IDENTITY` | `SEQUENCE` plus a `BEFORE INSERT` trigger |
| `priority_score` | Virtual column | `BEFORE INSERT OR UPDATE` trigger |

The schema itself is unaffected: the columns, their meanings, and their relationships are identical under either approach. Only the DDL syntax changes.

The target version is confirmed with `SELECT * FROM v$version;` before the DDL scripts are written.
