# Role Definitions and Use-Case Scenarios

> Request Management System
> This document defines the system actors, their permissions, and the core usage scenarios.

---

## 1. Actors

| Role | Position in System | Core Responsibility |
|---|---|---|
| **Customer** | External user | Submits requests, tracks the status of their own requests |
| **Product Owner (PO)** | Internal user | Evaluates requests, prioritizes them, converts them into workflows |
| **Developer** | Internal user | Executes assigned tasks, updates their status |
| **Admin** | System administrator | User management and system-wide visibility |

**Role separation principle:** Business roles (Customer, PO, Developer) are separated from the system administration role (Admin). PO is a business role — it has no administrative privileges such as creating users or changing roles. This separation prevents permission confusion and privilege escalation risks.

---

## 2. Permission Matrix

| Operation | Customer | PO | Developer | Admin |
|---|:---:|:---:|:---:|:---:|
| Create request | ✅ | ❌ | ❌ | ❌ |
| View own requests | ✅ | — | — | — |
| View all requests | ❌ | ✅ | ❌ | ✅ |
| View priority score | ❌ | ✅ | ✅ | ✅ |
| Prioritize (enter impact/urgency) | ❌ | ✅ | ❌ | ❌ |
| Reject request | ❌ | ✅ | ❌ | ❌ |
| Convert to workflow | ❌ | ✅ | ❌ | ❌ |
| Assign developer | ❌ | ✅ | ❌ | ❌ |
| Claim task (self-assign) | ❌ | ❌ | ✅ | ❌ |
| Update workflow status | ❌ | ❌ | ✅ | ❌ |
| View analytics reports | ❌ | ✅ | ❌ | ✅ |
| Create user / change role | ❌ | ❌ | ❌ | ✅ |
| Deactivate user / unlock account | ❌ | ❌ | ❌ | ✅ |
| Update own profile | ✅ | ✅ | ✅ | ✅ |

**Critical visibility rule:** A Customer can **never** see the priority score of their request (impact, urgency, priority_score). This information is the PO's internal resource-planning tool; exposing it to customers leads to disputes, score manipulation, and cross-customer comparison issues. The rule is enforced at two layers: (1) the DTO returned to Customers does not contain these fields at all, (2) access to the relevant endpoint is blocked via RBAC.

---

## 3. Use-Case Scenarios

### 3.1 Customer

---

#### UC-01 — Create New Request

| | |
|---|---|
| **Actor** | Customer |
| **Precondition** | User is logged in, role is `CUSTOMER`, account is active |
| **Trigger** | Customer opens the "New Request" screen |

**Main flow:**
1. Customer enters the request title (max. 200 characters)
2. Customer enters the request details/description
3. Customer clicks "Submit Request"
4. System takes `customer_id` from the session (values sent by the client are not trusted)
5. Request is saved with `status = NEW`
6. A success notification is shown to the customer

**Alternative flows:**
- **A1:** Title or description is empty → validation error, nothing is saved
- **A2:** Title exceeds 200 characters → validation error

**Postcondition:** The request enters the PO's prioritization pool.

**Business rules:**
- `customer_id` is always taken from the session owner, never from the request body
- `created_at` is set automatically upon creation

---

#### UC-02 — View Own Requests

| | |
|---|---|
| **Actor** | Customer |
| **Precondition** | User is logged in, role is `CUSTOMER` |
| **Trigger** | Customer opens the "My Requests" menu |

**Main flow:**
1. System lists only the requests belonging to the session owner's `customer_id`
2. For each request, the title, creation date, and a **user-friendly status message** are shown
3. If the request is `REJECTED`, the rejection reason (`rejection_reason`) is also displayed

**Status message mapping:**

| System status | Shown to customer |
|---|---|
| `NEW` | Your request has been received |
| `PRIORITIZED` | Your request is under evaluation |
| `IN_WORKFLOW` | Your request is being worked on |
| `CLOSED` | Your request has been completed |
| `REJECTED` | Your request was not taken forward + reason |

**Business rules:**
- A customer cannot access another customer's request, not even through URL/ID manipulation — the check is performed in the service layer; hiding it in the UI is not sufficient
- Priority score and assigned developer information **never appear** on this screen

---

#### UC-03 — Update Profile

| | |
|---|---|
| **Actor** | All roles |
| **Precondition** | User is logged in |

**Main flow:**
1. User updates their full name and/or email address
2. System checks email uniqueness
3. Changes are saved

**Alternative flow:**
- **A1:** Email already exists for another user → `DuplicateEmailException`, meaningful error message

**Business rules:**
- A user **cannot change their own role** — role changes are exclusively an Admin privilege
- Password change is not part of this flow; it is a separate flow requiring verification of the current password

---

### 3.2 Product Owner

---

#### UC-04 — View Prioritization Pool

| | |
|---|---|
| **Actor** | PO |
| **Precondition** | User is logged in, role is `PRODUCT_OWNER` |

**Main flow:**
1. PO opens the "Prioritization Pool" screen
2. System lists all requests with customer name, title, and priority score
3. Scored requests are sorted from highest to lowest
4. Requests not yet scored are shown at the bottom of the list labeled "Not Assigned"
5. Each row displays an action button appropriate to the request's status

**Action button logic:**

| Request status | Visible button |
|---|---|
| `NEW` | Prioritize |
| `PRIORITIZED` | Edit + Convert to Workflow |
| `IN_WORKFLOW` | (no action, read-only) |

**Business rules:**
- An unscored request = a request with no record in the `priorizations` table (no separate status field is needed)
- The list query runs as a single query using DTO projection to avoid the N+1 problem

---

#### UC-05 — Prioritize Request

| | |
|---|---|
| **Actor** | PO |
| **Precondition** | Request `status = NEW` (or `PRIORITIZED` when editing) |
| **Trigger** | PO clicks the "Prioritize" button |

**Main flow:**
1. PO is redirected to the detailed parameter entry screen
2. Business impact is selected — scale of 1-5 with descriptive options
3. Urgency is selected — scale of 1-5 with descriptive options
4. System updates the score preview live as selections change (impact × urgency)
5. PO clicks "Save Values"
6. A `priorizations` record is created, `prioritized_by` is set to the session owner
7. Request status is updated to `PRIORITIZED`
8. A notification is sent to the customer who submitted the request

**Alternative flows:**
- **A1:** Request is already prioritized → existing values are loaded into the form, the save is processed as an update (upsert)
- **A2:** Request is in `IN_WORKFLOW` status → prioritization cannot be edited, `InvalidStateTransitionException`

**Business rules:**
- `priority_score` is calculated at the database layer (trigger) — Java only sends impact and urgency, it never calculates and sends the score itself
- Impact and urgency cannot fall outside the 1-5 range (enforced by both a DB check constraint and application-level validation)
- Steps 6-8 execute atomically within a single transaction

**Score interpretation ranges:**

| Score | Label |
|---|---|
| 1-6 | Low |
| 7-15 | Medium |
| 16-25 | Critical |

---

#### UC-06 — Reject Request

| | |
|---|---|
| **Actor** | PO |
| **Precondition** | Request `status = NEW` or `PRIORITIZED` |

**Main flow:**
1. PO selects the "Reject" action
2. System requires a rejection reason (mandatory field)
3. **A confirmation dialog is shown** — the action is irreversible
4. PO confirms
5. Request transitions to `REJECTED`, `rejection_reason` is saved
6. A notification with the reason is sent to the customer

**Alternative flow:**
- **A1:** Request is in `IN_WORKFLOW` status → cannot be rejected, development has already started

**Business rules:**
- The rejection reason is mandatory and is shown to the customer
- `REJECTED` is a dead-end state with no path back
- Because it is irreversible, a confirmation dialog is mandatory

---

#### UC-07 — Convert to Workflow

| | |
|---|---|
| **Actor** | PO |
| **Precondition** | Request `status = PRIORITIZED` |

**Main flow:**
1. PO clicks the "Convert to Workflow" button
2. A `workflows` record is created with `workflow_status = BACKLOG`
3. Request status is updated to `IN_WORKFLOW`
4. An entry is written to the status history

**Alternative flow:**
- **A1:** Request is in `NEW` status (not prioritized) → `InvalidStateTransitionException`, steps cannot be skipped

**Business rules:**
- Steps 2-4 execute atomically within a single transaction — if the workflow is created but the request status is not updated, the system is left inconsistent
- Assigning a developer is not required at this stage; a task can wait in `BACKLOG` without one

---

#### UC-08 — Assign Developer

| | |
|---|---|
| **Actor** | PO |
| **Precondition** | A workflow record exists with `workflow_status = BACKLOG` |

**Main flow:**
1. PO selects a task from the backlog
2. PO assigns a user from the list of active developers
3. `workflows.developer_id` is updated
4. A notification is sent to the assigned developer

**Business rules:**
- Only users with `role = DEVELOPER` and `is_active = 1` can be assigned
- Row-level locking (`SELECT ... FOR UPDATE`) is used to prevent concurrent assignment conflicts on the same task

---

#### UC-09 — View Analytics Reports

| | |
|---|---|
| **Actor** | PO, Admin |

**Main flow:**
1. User opens the "Analytics Reports" screen
2. System visualizes the available reports

**Report set:**

| Report | Content |
|---|---|
| Monthly request volume | Number of requests created per month |
| Status distribution | Ratio of requests across statuses |
| Customer history | All requests and outcomes for a selected customer |
| Average resolution time | Average of `created_at` → `closed_at` difference |
| Developer performance | Completed task count and average duration per developer |
| Top requesting customers | Top 5 customers by request count |
| Test rework rate | Rate of `TESTING → IN_PROGRESS` transitions (quality indicator) |

**Business rules:**
- Aggregation calculations are performed in the PL/SQL view layer, not in the application layer
- Reports can be exported as CSV/Excel

---

### 3.3 Developer

---

#### UC-10 — View Task List

| | |
|---|---|
| **Actor** | Developer |
| **Precondition** | User is logged in, role is `DEVELOPER` |

**Main flow:**
1. Developer opens the task board
2. Tasks assigned to them are listed grouped by status
3. Additionally, unassigned backlog tasks are visible (available to claim)

**Business rules:**
- A developer can see the request's priority score (so they can work in priority order)
- Tasks assigned to another developer are read-only and cannot be updated

---

#### UC-11 — Claim Task (Self-Assign)

| | |
|---|---|
| **Actor** | Developer |
| **Precondition** | Task is in `BACKLOG` status with `developer_id = null` |

**Main flow:**
1. Developer selects an unassigned task
2. Developer triggers the "Claim" action
3. `workflows.developer_id` is set to the session owner

**Alternative flow:**
- **A1:** If the task has been claimed by another developer in the meantime → conflict error, the list is refreshed

**Business rules:**
- Row-level locking (`SELECT ... FOR UPDATE`) is used to prevent concurrent claim conflicts — two developers cannot claim the same task simultaneously

---

#### UC-12 — Update Task Status

| | |
|---|---|
| **Actor** | Developer |
| **Precondition** | Task is assigned to the session owner |

**Main flow:**
1. Developer selects the new status for the task
2. System validates whether the transition is allowed
3. If the transition is `DONE`, **a confirmation dialog is shown** (irreversible)
4. Status is updated, an entry is written to the status history
5. If the transition is `DONE`, the request automatically moves to `CLOSED` and `closed_at` is set
6. A completion notification is sent to the customer who submitted the request

**Allowed transitions:**

| Current status | Allowed target statuses |
|---|---|
| `BACKLOG` | `IN_PROGRESS` |
| `IN_PROGRESS` | `TESTING` |
| `TESTING` | `DONE`, `IN_PROGRESS` (test failed) |
| `DONE` | — (final state) |

**Alternative flows:**
- **A1:** Attempt to skip a step (e.g. `BACKLOG → DONE`) → `InvalidStateTransitionException`
- **A2:** Transition to `IN_PROGRESS` while `developer_id = null` → rejected, the task must be claimed first
- **A3:** Any transition out of `DONE` → rejected

**Business rules:**
- Transition rules are defined centrally inside the `WorkflowStatus` enum, not scattered across the service layer
- Steps 4-6 execute atomically within a single transaction
- Request closure after `DONE` is automatic and requires no PO approval

---

### 3.4 Admin

---

#### UC-13 — List and Create Users

| | |
|---|---|
| **Actor** | Admin |
| **Precondition** | User is logged in, role is `ADMIN` |

**Main flow:**
1. Admin opens the user management screen
2. All users are listed with role, active state, and lock state
3. Admin can create a new user: full name, email, role, security question and answer
4. System generates a temporary password and sets `must_change_password = 1`

**Business rules:**
- Email must be unique
- The security answer is not stored in plain text; it is hashed
- The user is forced to change their password on first login

---

#### UC-14 — Change Role and Deactivate User

| | |
|---|---|
| **Actor** | Admin |

**Main flow:**
1. Admin selects a user
2. Admin changes their role or updates their `is_active` state
3. Changes are saved

**Business rules:**
- **Users are never physically deleted** — deletion is performed by setting `is_active = 0`. Physical deletion would break `workflows.developer_id` and `priorizations.prioritized_by` references and corrupt historical data
- Role changes do not affect past records; consistency is preserved because relationships are established through `user_id`
- A deactivated user cannot log in, but their name continues to appear in historical records

---

#### UC-15 — Unlock Account and Reset Password

| | |
|---|---|
| **Actor** | Admin |
| **Precondition** | The user's account is locked (`is_locked = 1`) or a password reset is needed |

**Main flow:**
1. Admin filters for locked users
2. Admin triggers the "Unlock and Assign Password" action
3. System generates a temporary password and displays it on screen
4. `is_locked = 0`, `failed_reset_attempts = 0`, and `must_change_password = 1` are set

**Business rules:**
- This flow is the fallback mechanism for self-service password reset (UC-17)

---

#### UC-16 — System-Wide Visibility

| | |
|---|---|
| **Actor** | Admin |

**Main flow:**
1. Admin views all requests, workflows, and status history without restriction
2. Admin accesses system-wide statistics (user count, role distribution, active/inactive ratio)

**Business rules:**
- Admin has viewing privileges but **cannot bypass** the workflow state machine rules — Admin has no authority to prioritize requests or change workflow statuses. This preserves the separation between business roles and the system administration role

---

### 3.5 Authentication (All Roles)

---

#### UC-17 — Forgot Password (Self-Service)

| | |
|---|---|
| **Actor** | All roles |
| **Precondition** | The user account exists and is not locked |

**Main flow:**
1. User enters their email address
2. System displays the security question the user selected (not the answer)
3. User submits the answer
4. System validates the answer via hash comparison
5. If correct, the user is redirected to the new password screen
6. `failed_reset_attempts` is reset

**Alternative flows:**
- **A1:** Answer is incorrect → `failed_reset_attempts` is incremented, an error message is shown
- **A2:** Attempt limit exceeded → `is_locked = 1`, the account is locked, the user is directed to the Admin
- **A3:** Email does not exist in the system → the same generic message is shown for security reasons (prevents user enumeration)

**Business rules:**
- The security answer is stored hashed; no plain-text comparison is performed
- Security questions are selected from a fixed list (free-text entry is not allowed)
- The attempt counter and the locking operation execute within a single transaction

---

#### UC-18 — Forced Password Change

| | |
|---|---|
| **Actor** | All roles |
| **Precondition** | `must_change_password = 1` |

**Main flow:**
1. User logs in with the temporary password
2. System redirects them to the password change screen
3. User sets a new password
4. `must_change_password = 0` is set

**Business rules:**
- While the flag is active, the user cannot access any other screen in the system

---

## 4. Cross-Cutting Security Rules

Applicable across all scenarios:

1. **Session trust:** User identity (`user_id`, `role`) is always read from the session; values sent by the client are never trusted
2. **Layered authorization:** Permission checks are performed both at the endpoint level (`@PreAuthorize`) and in the service layer (ownership checks) — hiding a button in the UI is not sufficient
3. **Data visibility is constrained by DTOs:** Separate response DTOs are used for different roles; fields are never nulled out within a single shared DTO
4. **Inactive users have no access:** A user with `is_active = 0` cannot log in
5. **Irreversible actions require confirmation:** `DONE` and `REJECTED` transitions require a confirmation dialog
