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
| **Admin** | System administrator | Account administration: creation, roles, deactivation, unlocking |

**Role separation principle:** Business roles (Customer, PO, Developer) are separated from the system administration role (Admin). PO is a business role — it has no administrative privileges such as creating users or changing roles. This separation prevents permission confusion and privilege escalation risks.

---

## 2. Permission Matrix

| Operation | Customer | PO | Developer | Admin |
|---|:---:|:---:|:---:|:---:|
| Create request | ✅ | ❌ | ❌ | ❌ |
| View own requests | ✅ | — | — | — |
| View the request pool | ❌ | ✅ | ❌ | ❌ |
| View priority score | ❌ | ✅ | ✅ | ❌ |
| Prioritize (enter impact/urgency) | ❌ | ✅ | ❌ | ❌ |
| Reject request | ❌ | ✅ | ❌ | ❌ |
| Convert to workflow | ❌ | ✅ | ❌ | ❌ |
| View the unclaimed backlog | ❌ | ❌ | ✅ | ❌ |
| Claim task (self-assign) | ❌ | ❌ | ✅ | ❌ |
| Update workflow status | ❌ | ❌ | ✅ | ❌ |
| Create user / change role | ❌ | ❌ | ❌ | ✅ |
| Deactivate user / unlock account | ❌ | ❌ | ❌ | ✅ |
| Reissue a setup code | ❌ | ❌ | ❌ | ✅ |
| Update own profile and password | ✅ | ✅ | ✅ | ✅ |
| Read own notifications | ✅ | ✅ | ✅ | ✅ |

**Critical visibility rule:** A Customer can **never** see the priority score of their request (impact, urgency, priority_score). This information is the PO's internal resource-planning tool; exposing it to customers leads to disputes, score manipulation, and cross-customer comparison issues. The rule is enforced structurally: the DTO returned to a Customer does not declare these fields at all, so there is no value to omit on the way past.

**The administration role holds no business privileges.** An Admin manages who may use the system, not what the system decides — which is why the request pool and the priority score are closed to it as firmly as to a Customer. A role that could step around the state machine would make the state machine advisory. `RoleAccessTest` asserts these refusals for the Admin as explicitly as for everyone else.

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

#### UC-03 — Manage Own Account

| | |
|---|---|
| **Actor** | All roles |
| **Precondition** | User is logged in |

**Main flow — profile:**
1. User updates their full name and/or email address
2. System validates the address shape and checks uniqueness
3. Changes are saved

**Main flow — password:**
1. User supplies their current password and a new one
2. System verifies the current password against the stored hash
3. System applies the password rule to the new one and saves it

**Alternative flows:**
- **A1:** Email already used by another account → `DuplicateEmailException`
- **A2:** Email is malformed → `InvalidEmailException`
- **A3:** Current password is wrong → `IncorrectPasswordException`
- **A4:** New password fails the rule → `WeakPasswordException`

**Business rules:**
- A user **cannot change their own role** — the update form carries no role field at all, so one cannot be set by editing the request on its way to the server
- Uniqueness is checked only on a genuine change, or saving the form without touching the address would report a duplicate of yourself
- The two flows are kept separate: they ask for different things and fail for different reasons, and a form that changed a name and a password together would have to explain which half went wrong

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
- **A2:** Request is in `IN_WORKFLOW` status → prioritization cannot be edited, `PrioritizationNotEditableException`

**Business rules:**
- `priority_score` is a **virtual column** — `GENERATED ALWAYS AS (impact * urgency) VIRTUAL`. Java only sends impact and urgency; it never calculates or sends the score, so the two cannot disagree
- Impact and urgency cannot fall outside the 1-5 range (enforced by both a DB check constraint and application-level validation)
- Steps 6-8 execute atomically within a single transaction

**Score interpretation ranges:**

The band is derived from the score for display and for the deadline, and is never stored.

| Score | Band | Days allowed once scheduled |
|---|---|---|
| 1–6 | Low | 20 |
| 7–12 | Medium | 10 |
| 13–19 | High | 5 |
| 20–25 | Critical | 2 |

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
- **A1:** Request is in `NEW` status (not prioritized) → `InvalidRequestTransitionException`, steps cannot be skipped

**Business rules:**
- Steps 2-4 execute atomically within a single transaction — if the workflow is created but the request status is not updated, the system is left inconsistent
- Assigning a developer is not required at this stage; a task can wait in `BACKLOG` without one

---

### 3.3 Developer

---

#### UC-08 — View Task Board

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

#### UC-09 — Claim Task (Self-Assign)

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

#### UC-10 — Update Task Status

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
- **A1:** Attempt to skip a step (e.g. `BACKLOG → DONE`) → `InvalidWorkflowTransitionException`
- **A2:** Transition to `IN_PROGRESS` while `developer_id = null` → rejected, the task must be claimed first
- **A3:** Any transition out of `DONE` → rejected

**Business rules:**
- Transition rules are defined centrally inside the `WorkflowStatus` enum, not scattered across the service layer
- Steps 4-6 execute atomically within a single transaction
- Request closure after `DONE` is automatic and requires no PO approval

---

### 3.4 Admin

---

#### UC-11 — List and Create Users

| | |
|---|---|
| **Actor** | Admin |
| **Precondition** | User is logged in, role is `ADMIN` |

**Main flow:**
1. Admin opens the user management screen
2. All users are listed with role, active state, lock state, and whether the account is still awaiting setup
3. Admin filters by role or searches by name or email
4. Admin creates a new user with full name, email and role
5. System opens the account **without credentials** and returns a one-time setup code valid for seven days
6. Admin hands the code over; the code is shown once, in a dialog

**Alternative flows:**
- **A1:** Email already used by another account → `DuplicateEmailException`
- **A2:** Email is malformed → `InvalidEmailException`

**Business rules:**
- The email is validated and checked for uniqueness in the service as well as the form, because a caller reaching the service directly never saw the form — though the unique index is what actually guarantees it
- An account is created with **no password and no security question**. The person who will use the account chooses both (UC-15), so an administrator never learns a lasting credential belonging to someone else
- The search matches name **or** email, because an administrator looking for someone has one or the other

---

#### UC-12 — Change Role, Deactivate and Reactivate

| | |
|---|---|
| **Actor** | Admin |

**Main flow:**
1. Admin opens an account from the user list
2. Admin changes the role, or deactivates or reactivates the account
3. Changes are saved

**Alternative flow:**
- **A1:** The subject is the last remaining active Admin → `CannotDemoteLastAdminException`, nothing is saved

**Business rules:**
- **Users are never physically deleted** — deactivation sets `is_active = 0`. Physical deletion would break the `workflows.developer_id`, `priorizations.prioritized_by` and `request_status_history.changed_by` references, which is to say it would erase the record of work performed by whoever left
- The last-administrator guard counts the *other* active administrators, so acting on a colleague is permitted where acting on oneself as the last administrator is not
- Role changes do not affect past records; relationships are established through `user_id`, so past work keeps the author it had
- A deactivated user cannot log in, but their name continues to appear in historical records

---

#### UC-13 — Unlock Account

| | |
|---|---|
| **Actor** | Admin |
| **Precondition** | The account is locked (`is_locked = 1`) |

**Main flow:**
1. Admin opens the locked account from the user list
2. Admin triggers the "Unlock" action
3. `is_locked = 0` and `failed_reset_attempts = 0` are set

**Business rules:**
- Unlocking clears the lock and the attempt counter and **nothing else**. The account keeps its password, because being locked out of recovery says nothing about whether the owner still knows how to sign in
- Locked and inactive are distinct states with distinct causes: locked is what the system does after too many failed recovery answers, inactive is what an administrator decided. Unlocking does not reactivate, and reactivating does not unlock
- Someone who has genuinely forgotten their password needs a new setup code (UC-14), which is a separate decision

---

#### UC-14 — Reissue a Setup Code

| | |
|---|---|
| **Actor** | Admin |
| **Precondition** | The account exists |

**Main flow:**
1. Admin opens the account from the user list
2. Admin triggers the "Reissue setup code" action
3. System issues a fresh one-time code valid for seven days and displays it once
4. The account's password, security question and answer are discarded

**Business rules:**
- Used when the first code expired or went astray, and as the fallback for an account that locked itself out of self-service recovery (UC-16)
- A reissued code is a fresh start, not a second key: whatever the account held before stops working
- The account returns to the awaiting-setup state, and its holder sets their own credentials again through UC-15

---

### 3.5 Authentication (All Roles)

---

#### UC-15 — Set Up an Account from a One-Time Code

| | |
|---|---|
| **Actor** | The holder of a setup code |
| **Precondition** | The account is active and holds an unexpired setup code |
| **Trigger** | The holder opens the account setup screen |

**Main flow:**
1. Holder enters the setup code
2. System confirms the code is usable and shows whose account it opens
3. Holder chooses a password, a security question from the fixed list, and its answer
4. System applies the password rule
5. Password and answer are stored hashed, the question is stored as its enum constant
6. The setup code is destroyed

**Alternative flows:**
- **A1:** Code is unknown, expired, or belongs to an inactive account → `InvalidSetupCodeException`
- **A2:** Password fails the rule → `WeakPasswordException`, the code remains usable

**Business rules:**
- The code is the only credential this flow accepts, which is why it is single-use and why every way of failing looks the same from outside
- Usability is checked **before** the form is shown, so a bad code is turned away at the door rather than after a password has been typed
- Steps 3–6 execute in a single transaction: there is no moment at which both the code and the password open the account
- Codes are compared after trimming and upper-casing, and the security answer after trimming and lower-casing, so a stray space or a capital letter cannot lock someone out of their own account
- This flow is reachable without signing in, because the person using it has no way to sign in yet

---

#### UC-16 — Recover a Forgotten Password

| | |
|---|---|
| **Actor** | All roles |
| **Precondition** | The account exists, is active, and has completed setup |

**Main flow:**
1. User enters their email address
2. System displays the security question the user chose (never the answer)
3. User submits the answer and a new password
4. System applies the password rule, then compares the answer by hash
5. If it matches, the new password is stored and `failed_reset_attempts` is reset

**Alternative flows:**
- **A1:** Answer is incorrect → `failed_reset_attempts` is incremented, `InvalidSecurityAnswerException`
- **A2:** Attempt limit (three) exceeded → `is_locked = 1`, `AccountLockedException`, the user is directed to an Admin
- **A3:** Email is not registered → a plausible question is shown and the submission fails identically to a wrong answer, so the form cannot be used to discover which addresses exist

**Business rules:**
- The security answer is stored hashed; no plain-text comparison is performed
- Security questions come from a fixed list; free-text entry is not allowed
- The question shown for an unknown address is derived deterministically from the address, so the same unknown address always gets the same one — a question that changed between attempts would give the pretence away
- The password rule is checked **before** the answer is compared, so someone who answered correctly does not lose an attempt because they also chose a short password
- The failed-attempt counter is written in a **separate transaction**, because the calling transaction is rolled back by the exception that reports the failure — an attempt that rolled back is an attempt that never happened
- This flow is reachable without signing in, which makes it the most exposed surface in the system and is why it is careful about what it reveals

---

## 4. Cross-Cutting Security Rules

Applicable across all scenarios:

1. **Session trust:** User identity (`user_id`, `role`) is always resolved from the security context; no service method accepts a user id from its caller, so there is no signature through which one user could act as another
2. **Layered authorization:** Routes declare `@RolesAllowed` and services declare `@PreAuthorize`. A view that forgets its annotation is one mistake; a service anyone reaching it can call is a hole that outlives the screen in front of it. Hiding a button is never the access control
3. **Ownership is a second question:** A role check answers *who is asking*, not *whose data*. Where it matters, ownership is checked inside the method, and a record belonging to someone else fails identically to one that does not exist — so ids cannot be probed
4. **Data visibility is constrained by DTOs:** Separate records are returned to different roles; fields are never nulled out within a single shared DTO
5. **Inactive users have no access:** A user with `is_active = 0` cannot log in
6. **Irreversible actions require confirmation:** the `DONE` and `REJECTED` transitions each open a confirmation dialog; reversible ones do not
