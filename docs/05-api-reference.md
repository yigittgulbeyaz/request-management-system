# API Reference

> Request Management System
> This document is the reference for the application's programmatic surface: the
> service layer, the data transfer objects that cross it, the enums that model
> the domain, and the error codes it reports.

---

## 1. Scope

**There is no HTTP API.** The user interface is built with Vaadin Flow, which
renders from Java on the server, so no REST or GraphQL endpoint is published and
no OpenAPI document exists. The browser talks to Vaadin's own protocol at
`/VAADIN/**`, which is framework-internal and not an application interface.

The application's API is therefore its **service layer**. Every screen reaches
the domain through one of the services in Section 4, and those services are the
contract this document describes. That contract is what the test suite asserts:
`RoleAccessTest` tests the authorization column, `WorkflowServiceTest` and
`RequestServiceTest` test the behaviour, and `TransactionBoundaryTest` tests the
atomicity claims of Section 7.

If an HTTP API is ever added, it belongs *in front of* these services rather
than beside them. The rules live in the service layer, so a controller that
called repositories directly would bypass every guarantee below.

---

## 2. Conventions

These hold for every method in Section 4 and are not repeated per entry.

| Convention | Detail |
|---|---|
| **Caller identity** | Never a parameter. `CurrentUserService` resolves the acting user from the Spring Security context. No service method accepts a user id from its caller, so there is no signature through which one user could act as another. |
| **Authorization** | Declared with `@PreAuthorize`, on the class where every method belongs to one role and per method otherwise. Refused calls raise `AccessDeniedException` before the method body runs. Views additionally declare `@RolesAllowed`; see §3. |
| **Ownership** | A role check answers *who is asking*, not *whose data*. Where it matters, ownership is a second check inside the method — and a record belonging to someone else fails identically to one that does not exist, so ids cannot be probed. |
| **Transactions** | `@Transactional` on every writing method, `@Transactional(readOnly = true)` on every reading one. Boundaries are drawn around a whole user action; see §7. |
| **Errors** | Every failure is a `BaseException` subclass carrying a stable error code (§6). Services throw; nothing catches. `GlobalErrorHandler` turns the code into a sentence for the user. |
| **Paging** | List methods take a `Pageable` and return a `List`, never a `Page`. The count is a separate method, because the grid asks for it separately and a `Page` would issue a `COUNT` on every fetch for a total nobody reads. |
| **Projections** | Reads return records built by JPQL constructor expressions, not entities. One query per page regardless of row count, and no entity escapes the transaction that loaded it. |
| **Nullable filters** | A `null` filter argument means "no filter". One query serves both the filtered and unfiltered view. |

---

## 3. Authorization Map

`ROLE_` prefixes are supplied by `Role.asAuthority()`; `@PreAuthorize` uses the
bare name via `hasRole(...)`.

| Service | Required authority | Declared |
|---|---|---|
| `RequestService` | `CUSTOMER` | class |
| `PoRequestService` | `PRODUCT_OWNER` | class |
| `PrioritizationService` | `PRODUCT_OWNER` | class |
| `WorkflowService` | `PRODUCT_OWNER` for conversion, `DEVELOPER` for everything else | per method |
| `AdminUserService` | `ADMIN` | class |
| `NotificationService` | any authenticated user | class (`isAuthenticated()`) |
| `ProfileService` | any authenticated user | none — enforced by `CurrentUserService` |
| `AccountSetupService` | **none** — reachable anonymously | — |
| `PasswordRecoveryService` | **none** — reachable anonymously | — |
| `RequestAuditService` | **none** — internal, called by services that already checked | — |

The two anonymous services are deliberate: the person using them has no account
to sign in with yet, or cannot sign in. They are the most exposed surface in the
system and are correspondingly careful about what they reveal (§4.7, §4.8).

---

## 4. Service Reference

Exceptions listed are those the method raises itself. `AccessDeniedException`
from the authorization layer and `UnauthenticatedException` from
`CurrentUserService` are omitted throughout.

### 4.1. `RequestService` — a customer's own requests

Package `com.yigit.requestms.request.service` · role `CUSTOMER`

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `submit(RequestCreateDto)` | write | `Long` — new request id | — |
| `listMyRequests(Pageable)` | read | `List<CustomerRequestDto>` | — |
| `countMyRequests()` | read | `long` | — |
| `getMyRequest(Long)` | read | `CustomerRequestDetailDto` | `RequestNotFoundException` |
| `getMyRequestTimeline(Long)` | read | `List<StatusTimelineEntryDto>` | `RequestNotFoundException` |

`submit` returns the id rather than the entity: handing back a managed entity
would let the caller modify it outside the transaction that created it. The
customer is taken from the session, so `RequestCreateDto` carries no customer
field.

Every read is scoped to the session owner. `getMyRequest` queries by id **and**
customer id together, so another customer's request raises
`RequestNotFoundException` — the same failure as a request that does not exist.

`getMyRequestTimeline` returns only the five request-level states. The audit
table also holds workflow stages, and a customer being shown `TESTING` would be
told something they were never meant to see.

### 4.2. `PoRequestService` — the prioritization pool

Package `com.yigit.requestms.request.service` · role `PRODUCT_OWNER`

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `listPool(RequestStatus, Pageable)` | read | `List<RequestSummaryDto>` | — |
| `countPool(RequestStatus)` | read | `long` | — |
| `reject(Long, String reason)` | write | `void` | `RejectionReasonRequiredException`, `RequestNotFoundException`, `InvalidRequestTransitionException` |

`listPool` accepts a `null` status for the unfiltered view. Default ordering is
score descending with **unscored requests last** rather than treated as zero: no
score means nobody has judged the request yet, which is work to do rather than
work to defer. A sort supplied on the `Pageable` replaces that default, and a
sort on the score column is rewritten to keep nulls last.

`reject` requires a non-blank reason and carries it into both the record and the
customer's notification. `REJECTED` is a final state with no path back.

### 4.3. `PrioritizationService` — scoring

Package `com.yigit.requestms.prioritization.service` · role `PRODUCT_OWNER`

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `isScorable(Long)` | read | `boolean` | — |
| `loadForScoring(Long)` | read | `PrioritizationDetailDto` | `RequestNotFoundException`, `PrioritizationNotEditableException` |
| `score(Long, PrioritizationFormDto)` | write | `void` | `RequestNotFoundException`, `PrioritizationNotEditableException`, `InvalidRequestTransitionException` |

`isScorable` exists because navigation needs a yes or no before opening the
form; an exception is not an answer a router can act on. The same rule is
checked again inside `loadForScoring` and `score`, because a caller reaching the
service directly never went through the router.

A request is scorable while its status is `NEW` or `PRIORITIZED`. Scoring stops
once development starts — changing the number afterwards would rewrite the
reason the work was scheduled, after the fact.

`score` is an upsert. First scoring inserts, moves the request to
`PRIORITIZED`, writes an audit entry and notifies the customer. A **revision**
updates the existing row only: it is a correction to a decision already
recorded, not a new event, so it writes no audit entry and sends no notice, and
`prioritized_by` and `created_at` keep describing the original scoring.

The form carries `ImpactLevel` and `UrgencyLevel`, never a score. The score is a
virtual column computed by the database as `impact * urgency`.

### 4.4. `WorkflowService` — development tasks

Package `com.yigit.requestms.workflow.service` · roles per method

| Method | Role | Tx | Returns | Throws |
|---|---|---|---|---|
| `convertToWorkflow(Long)` | `PRODUCT_OWNER` | write | `Long` — new task id | `RequestNotFoundException`, `WorkflowAlreadyExistsException`, `InvalidRequestTransitionException` |
| `listMyTasks(WorkflowStatus, Pageable)` | `DEVELOPER` | read | `List<TaskSummaryDto>` | — |
| `countMyTasks(WorkflowStatus)` | `DEVELOPER` | read | `long` | — |
| `listUnclaimed(Pageable)` | `DEVELOPER` | read | `List<TaskSummaryDto>` | — |
| `countUnclaimed()` | `DEVELOPER` | read | `long` | — |
| `claim(Long)` | `DEVELOPER` | write | `void` | `TaskNotFoundException`, `TaskAlreadyClaimedException` |
| `advance(Long, WorkflowStatus)` | `DEVELOPER` | write | `void` | `TaskNotFoundException`, `TaskNotAssignedToYouException`, `InvalidWorkflowTransitionException` |

The split of roles is the pull model: scheduling work is the product owner's
decision, everything after it is the developer's. `convertToWorkflow` assigns
nobody — the task waits in `BACKLOG` until a developer takes it.

`convertToWorkflow` derives the deadline from the request's priority band and
stores it. The score is read inside the method rather than passed in, so a
caller cannot supply one the database does not hold. An unscored request
reaching conversion is a programming error and raises `IllegalStateException`,
not a domain exception — the state machine should have refused it already.

`claim` reads the row with `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) and
holds it for the whole of the check and the write. Two developers claiming at
the same instant produce one success and one `TaskAlreadyClaimedException`;
without the lock the second write would silently replace the first and the loser
would never be told. `WorkflowConcurrencyTest` asserts this against the real
database.

`advance` checks two separate things: the role says a developer may move tasks,
the ownership check says it must be their own. Ownership compares **ids**, not
entities — the developer association is lazy, so the getter returns a proxy
whose own field is unpopulated and entity equality would read a null id and
refuse a task the caller does own.

Reaching `DONE` also closes the linked request, sets `closed_at`, writes a
second audit entry and notifies the customer, all in the same transaction. No
approval step stands in front of it, because there is no reviewer role for one
to belong to.

### 4.5. `AdminUserService` — account administration

Package `com.yigit.requestms.admin.service` · role `ADMIN`

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `list(Role, String search, Pageable)` | read | `List<AdminUserDto>` | — |
| `count(Role, String search)` | read | `long` | — |
| `detail(Long)` | read | `UserDetailDto` | `UserNotFoundException` |
| `create(CreateUserDto)` | write | `CreatedUserDto` — includes the one-time code | `InvalidEmailException`, `DuplicateEmailException` |
| `reissueSetupCode(Long)` | write | `CreatedUserDto` | `UserNotFoundException` |
| `changeRole(Long, Role)` | write | `void` | `UserNotFoundException`, `CannotDemoteLastAdminException` |
| `deactivate(Long)` | write | `void` | `UserNotFoundException`, `CannotDemoteLastAdminException` |
| `reactivate(Long)` | write | `void` | `UserNotFoundException` |
| `unlock(Long)` | write | `void` | `UserNotFoundException` |

Both filters on `list` are optional; `search` matches name **or** email, since
an administrator looking for someone has one or the other.

`create` opens the account **without credentials** and returns a one-time setup
code valid for seven days. What an administrator gets back is a code to hand
over, not a password to remember on someone else's behalf. The email is
validated and checked for uniqueness here as well as in the form, because a
caller reaching the service directly never saw the form — though the unique
index is what actually guarantees it, since two administrators creating the same
address at the same moment would both pass the check.

`reissueSetupCode` discards whatever the account had, password included: a
reissued code is a fresh start, not a second key.

`changeRole` and `deactivate` refuse to leave the system without an
administrator. The check counts the *other* active administrators, so acting on
a colleague is permitted where acting on oneself as the last administrator is
not.

`deactivate` is a soft delete (`is_active = 0`). Nothing is ever removed — the
foreign keys in `workflows`, `priorizations` and `request_status_history` would
lose the record of work performed by whoever left.

`unlock` clears the lock and the attempt counter and **nothing else**. The
account keeps its password: being locked out of recovery says nothing about
whether the owner still knows how to sign in. Someone who has genuinely
forgotten needs a new setup code, which is a separate decision. Locked and
inactive are different states with different causes — unlocking does not
reactivate, and reactivating does not unlock.

Nothing here touches requests, scores or tasks. An administrator manages who may
use the system, not what the system decides.

### 4.6. `ProfileService` — the caller's own account

Package `com.yigit.requestms.user.service` · any authenticated user

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `load()` | read | `ProfileDto` | — |
| `update(ProfileUpdateDto)` | write | `void` | `InvalidEmailException`, `DuplicateEmailException` |
| `changePassword(PasswordChangeDto)` | write | `void` | `IncorrectPasswordException`, `WeakPasswordException` |

`ProfileUpdateDto` carries a name and an email and **no role**. Leaving the
field out is the whole protection: one that never arrives cannot be set by
someone editing the request on its way to the server. Only an administrator
changes a role.

Uniqueness is checked only on a genuine change, or saving a form without
touching the address would report a duplicate of yourself.

`changePassword` is separate from `update` rather than folded into one save.
They ask for different things and fail for different reasons, and a form that
changed a name and a password together would have to explain which half went
wrong.

### 4.7. `AccountSetupService` — claiming an account

Package `com.yigit.requestms.auth.service` · **anonymous**

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `isUsable(String setupCode)` | read | `boolean` | — |
| `nameFor(String setupCode)` | read | `String` | `InvalidSetupCodeException` |
| `complete(AccountSetupDto)` | write | `void` | `InvalidSetupCodeException`, `WeakPasswordException` |

The code is the only credential, which is why it is single-use and why every way
of failing looks the same from outside. `isUsable` is asked before the form is
shown, so a bad code is turned away at the door rather than after a password has
been typed. A code is usable when it matches an **active** account and has not
expired.

Codes are compared after trimming and upper-casing, because anyone typing one
back may use lower case or leave the grouping dashes out. The security answer is
stored after trimming and lower-casing, so a capital letter cannot lock someone
out of their own account.

`complete` sets the password, the question and the answer and destroys the code
**in the same transaction**. There is no moment at which both the code and the
password open the account.

### 4.8. `PasswordRecoveryService` — a forgotten password

Package `com.yigit.requestms.auth.service` · **anonymous**

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `challengeFor(String email)` | read | `RecoveryChallengeDto` | — |
| `recover(PasswordRecoveryDto)` | write | `void` | `WeakPasswordException`, `InvalidSecurityAnswerException`, `AccountLockedException` |

`challengeFor` **never** reveals whether an account exists. An unknown address
is shown a plausible question derived deterministically from the address itself,
so the same unknown address always gets the same one — a question that changed
between attempts would give the pretence away. `RecoveryChallengeDto.accountExists`
is for the service's own use and must not be rendered.

`recover` fails identically for an unknown address and a wrong answer. Order
matters: the password rule is checked **before** the answer is compared, so
someone who answered correctly does not lose an attempt because they also chose
a short password.

Three wrong answers lock the account. The counter is incremented in a **separate
transaction** (`REQUIRES_NEW`, in `RecoveryAttemptRecorder`) because the calling
transaction is about to be rolled back by the exception that reports the
failure — an attempt that rolled back is an attempt that never happened. The
recorder is a separate class because Spring's propagation is proxy-based and a
call to a method on the same object would never cross the proxy.

### 4.9. `NotificationService` — the caller's own notices

Package `com.yigit.requestms.notification.service` · any authenticated user

| Method | Tx | Returns | Throws |
|---|---|---|---|
| `recent()` | read | `List<NotificationDto>` — newest first, at most 15 | — |
| `unreadCount()` | read | `long` | — |
| `markAllRead()` | write | `void` | — |

Fifteen is enough to see what happened while you were away without turning a
menu into a screen; anything older is in the request itself.

`markAllRead` is a deliberate action, not a side effect of opening the menu.
Opening it to check something is not the same as having dealt with what is in
it, and a badge that clears itself on a glance stops meaning anything. It runs
as one bulk `UPDATE` rather than loading every unread row to flip a flag.

### 4.10. Internal services

Not part of the surface a screen calls, but part of the contract.

| Type | Method | Notes |
|---|---|---|
| `RequestAuditService` | `recordTransition(RequestEntity, Enum<?> from, Enum<?> to, UserEntity actor)` | Writes one audit row. **No `@Transactional` of its own** — it joins the transaction of the change it describes, so an audit row cannot survive a rolled-back change. |
| | `notify(UserEntity recipient, String message, RequestEntity related)` | Writes one notification, same transaction as the change. |
| `CurrentUserService` | `requireId()` → `Long` | Reads the id from the security context. Throws `UnauthenticatedException`. |
| | `require()` → `UserEntity` | Loads the acting user. Throws `UnauthenticatedException` if the row is gone, which soft delete is supposed to make impossible. |
| `AppUserDetailsService` | `loadUserByUsername(String email)` | Spring Security SPI. Throws `UsernameNotFoundException`. |
| `RecoveryAttemptRecorder` | `recordFailure(Long userId)` | `REQUIRES_NEW`. Package-private; see §4.8. |
| `SetupCodeGenerator` | `generate()` → `String` | `SecureRandom`, three groups of four, alphabet without `O 0 l 1 I`. Package-private. |

`RequestAuditService` carries no `@PreAuthorize` either: it is called by
services that have already checked, and demanding a role would demand one from
the system writing its own record.

### 4.11. Validation policies

Static, stateless, and the single definition of two rules. The forms call them
to explain a problem; the services call them to refuse, which is the one that
counts.

| Type | Method | Rule |
|---|---|---|
| `PasswordPolicy` | `require(String)` | At least 8 characters, including a letter and a digit. Throws `WeakPasswordException`. |
| | `isAcceptable(String)` → `boolean` | Same rule, no exception. |
| | `describe()` → `String` | Human-readable form of the rule, for the field hint. |
| `EmailPolicy` | `require(String)` | Matches `^[^\s@]+@[^\s@]+\.[^\s@]{2,}$`. Throws `InvalidEmailException`. |
| | `isWellFormed(String)` → `boolean` | Same rule, no exception. |
| | `describe()` → `String` | Human-readable form of the rule. |

The email pattern is deliberately permissive: one strict enough to reject every
invalid address also rejects valid ones nobody expected, and the only real proof
an address works is sending to it.

---

## 5. Data Transfer Objects

All are Java records — immutable by construction. Grouped by the service that
returns or accepts them.

### 5.1. Requests

| Record | Fields |
|---|---|
| `RequestCreateDto` | `title` *(5–200, `@NotBlank`)*, `description` *(20–4000, `@NotBlank`)* |
| `CustomerRequestDto` | `id`, `title`, `status`, `rejectionReason`, `createdAt` |
| `CustomerRequestDetailDto` | `id`, `title`, `description`, `status`, `rejectionReason`, `createdAt`, `closedAt` |
| `RequestSummaryDto` | `id`, `customerName`, `title`, `priorityScore`, `status`, `createdAt` |
| `StatusTimelineEntryDto` | `newStatus` *(String, not an enum — the trail records both machines)*, `changedAt` |

The customer-facing records **do not declare** an impact, an urgency or a score.
There is no field to null out on the way past, which is how the visibility rule
is enforced structurally rather than by remembering to hide a column.

### 5.2. Prioritization

| Record | Fields |
|---|---|
| `PrioritizationFormDto` | `impact` *(`ImpactLevel`)*, `urgency` *(`UrgencyLevel`)* |
| `PrioritizationDetailDto` | `requestId`, `requestTitle`, `customerName`, `description`, `impact`, `urgency`, `priorityScore`; helper `isScored()` |

`PrioritizationFormDto` carries levels, never a number, and no score field at
all — the database derives it.

### 5.3. Workflow

| Record | Fields |
|---|---|
| `TaskSummaryDto` | `taskId`, `requestId`, `requestTitle`, `priorityScore`, `status`, `developerName`, `createdAt`, `deadline`; helpers `isOverdue()`, `isDueSoon()` *(within two days)* |

`deadline` is nullable — tasks converted before the rule existed have none, and
saying so is better than showing a date nobody promised. A task is never overdue
once it is `DONE`: delivered late is late, but it is not still running out of
time.

### 5.4. Accounts

| Record | Fields |
|---|---|
| `AdminUserDto` | `id`, `nameSurname`, `email`, `role`, `active`, `locked`, `awaitingSetup`, `createdAt` |
| `UserDetailDto` | as above plus `failedResetAttempts`, `securityQuestion` |
| `CreateUserDto` | `nameSurname`, `email`, `role` |
| `CreatedUserDto` | `userId`, `email`, `setupCode`, `expiresAt` |
| `ProfileDto` | `nameSurname`, `email`, `role`, `securityQuestion` |
| `ProfileUpdateDto` | `nameSurname`, `email` — **no role** |
| `PasswordChangeDto` | `currentPassword`, `newPassword` |
| `AccountSetupDto` | `setupCode`, `password`, `securityQuestion`, `securityAnswer` |
| `PasswordRecoveryDto` | `email`, `securityAnswer`, `newPassword` |
| `RecoveryChallengeDto` | `question`, `accountExists` — the flag is internal, never rendered |

`awaitingSetup` is derived from the presence of a setup token rather than stored
twice: an account has credentials or it has a code, never both.

`CreatedUserDto.setupCode` is the only place a code is ever returned, and the
dialog that shows it is the only place it is ever displayed.

### 5.5. Notifications

| Record | Fields |
|---|---|
| `NotificationDto` | `id`, `message`, `read`, `relatedRequestId`, `createdAt` |

`relatedRequestId` is nullable and enables click-through to the request.

---

## 6. Enums

| Enum | Constants | Helpers |
|---|---|---|
| `Role` | `CUSTOMER`, `PRODUCT_OWNER`, `DEVELOPER`, `ADMIN` | `asAuthority()` → `ROLE_` + name |
| `RequestStatus` | `NEW`, `PRIORITIZED`, `IN_WORKFLOW`, `CLOSED`, `REJECTED` | `allowedTransitions()`, `canTransitionTo(...)`, `isFinal()` |
| `WorkflowStatus` | `BACKLOG`, `IN_PROGRESS`, `TESTING`, `DONE` | same three |
| `ImpactLevel` | `COSMETIC` 1, `MINOR` 2, `MODERATE` 3, `MAJOR` 4, `CRITICAL` 5 | `getValue()`, `getLabel()`, `ofValue(int)` |
| `UrgencyLevel` | `BACKLOG` 1, `LONG_TERM` 2, `MEDIUM_TERM` 3, `SHORT_TERM` 4, `IMMEDIATE` 5 | `getValue()`, `getLabel()`, `ofValue(int)` |
| `PriorityBand` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | `getLabel()`, `getAllowedDays()`, `ofScore(int)` |
| `SecurityQuestion` | `FIRST_PET`, `BIRTH_CITY`, `PRIMARY_SCHOOL_TEACHER`, `FAVOURITE_BOOK` | `messageKey()` |

Both scoring enums number **1 = least, 5 = most**. `UrgencyLevel.BACKLOG` is
therefore the *lowest* urgency and `IMMEDIATE` the highest; the constant names
describe the timeline the work is expected to fit, and each carries a
descriptive label because "3" on its own is a matter of opinion while "next
sprint" is a question with an answer.

The transition tables live inside `RequestStatus` and `WorkflowStatus` as
exhaustive switches, so the rule is written once and adding a status without
handling it is a compile error. The developer board renders one button per
`allowedTransitions()` entry, so a change to the rules reaches the screen
without the screen being edited. Full matrices are in
[State Diagrams](02-state-diagrams.md).

`PriorityBand` is derived from the score for display and for the deadline, and
is **never stored** — a band is a way of reading the number, not a second fact
about the request.

| Score | Band | Days allowed |
|---|---|---|
| 1–6 | `LOW` | 20 |
| 7–12 | `MEDIUM` | 10 |
| 13–19 | `HIGH` | 5 |
| 20–25 | `CRITICAL` | 2 |

`SecurityQuestion` stores the constant name rather than the question text, so
the question can be rendered in whichever language the user has selected.

---

## 7. Transaction Boundaries

A boundary is drawn around the whole of what a user action means, not around
each write. These are the multi-table operations and what each must contain.

| Operation | Written together |
|---|---|
| `PrioritizationService.score` *(first scoring)* | score row · request status · audit entry · customer notification |
| `PrioritizationService.score` *(revision)* | score row only — no audit entry, no notification |
| `PoRequestService.reject` | request status · rejection reason · audit entry · customer notification |
| `WorkflowService.convertToWorkflow` | request status · audit entry · task row with its derived deadline |
| `WorkflowService.claim` | assignee · assignment timestamp, under a row lock |
| `WorkflowService.advance` → `DONE` | task stage · request status · `closed_at` · two audit entries · customer notification |
| `AccountSetupService.complete` | password · question · answer · destruction of the setup code |
| `RecoveryAttemptRecorder.recordFailure` | attempt counter · lock flag — in a **separate** transaction, deliberately |

If any step fails the whole transition rolls back. A partially applied
transition — a task created but the request still reading `PRIORITIZED`, or an
audit row describing a change that was undone — would leave the system saying
something untrue about itself.

---

## 8. Error Codes

Every domain exception extends `BaseException`, which carries a stable code and
extends `RuntimeException` — Spring does not roll back for a checked exception,
so a checked hierarchy could leave half-applied work committed. The code is not
an HTTP status: the entities that throw these have no business knowing HTTP
exists.

`ErrorMessages.forCode(...)` maps each to the sentence the user reads.
`ErrorMessagesTest` walks every exception class in the codebase and asserts its
code has a message, because adding one without a message is otherwise silent —
the user simply sees the generic fallback.

| Code | Exception | Shown to the user |
|---|---|---|
| `REQUEST_NOT_FOUND` | `RequestNotFoundException` | That request could not be found. |
| `INVALID_REQUEST_TRANSITION` | `InvalidRequestTransitionException` | This request can no longer move to that state. |
| `REJECTION_REASON_REQUIRED` | `RejectionReasonRequiredException` | A reason is required before rejecting a request. |
| `PRIORITIZATION_NOT_EDITABLE` | `PrioritizationNotEditableException` | This request can no longer be scored. |
| `WORKFLOW_ALREADY_EXISTS` | `WorkflowAlreadyExistsException` | This request is already in development. |
| `INVALID_WORKFLOW_TRANSITION` | `InvalidWorkflowTransitionException` | This task cannot move to that stage. |
| `TASK_NOT_FOUND` | `TaskNotFoundException` | That task could not be found. |
| `TASK_ALREADY_CLAIMED` | `TaskAlreadyClaimedException` | Someone else took this task first. |
| `TASK_NOT_YOURS` | `TaskNotAssignedToYouException` | This task is assigned to another developer. |
| `UNAUTHENTICATED` | `UnauthenticatedException` | Your session has ended. Please sign in again. |
| `USER_NOT_FOUND` | `UserNotFoundException` | That user could not be found. |
| `DUPLICATE_EMAIL` | `DuplicateEmailException` | An account already uses that email address. |
| `INVALID_EMAIL` | `InvalidEmailException` | That does not look like an email address. |
| `LAST_ADMIN_PROTECTED` | `CannotDemoteLastAdminException` | The only remaining administrator cannot be demoted or deactivated. |
| `INVALID_SETUP_CODE` | `InvalidSetupCodeException` | That setup code is not valid. Ask an administrator for a new one. |
| `WEAK_PASSWORD` | `WeakPasswordException` | A password needs at least 8 characters, including a letter and a digit. |
| `INVALID_SECURITY_ANSWER` | `InvalidSecurityAnswerException` | That answer does not match. |
| `ACCOUNT_LOCKED` | `AccountLockedException` | Too many incorrect answers. Ask an administrator to unlock the account. |
| `INCORRECT_PASSWORD` | `IncorrectPasswordException` | The current password is not correct. |

Anything that is **not** a `BaseException` is a defect: the user is shown
*"Something went wrong. Please try again."* and the detail goes to the log,
where it is of use to someone who can act on it.

`GlobalErrorHandler` walks the cause chain before matching, because Vaadin wraps
whatever a component listener throws — sometimes more than once.

---

## 9. Repository Queries

The persistence surface the services call. Every list method is a JPQL
constructor expression returning records, so a page costs one statement
regardless of row count; `RequestRepositoryQueryCountTest` measures this.

| Repository | Method | Notes |
|---|---|---|
| `RequestRepository` | `findSummariesByCustomer(Long, Pageable)` | Excludes the `CLOB` description. |
| | `countByCustomerId(Long)` | |
| | `findByIdAndCustomerId(Long, Long)` | Ownership is part of the query, not a check performed after loading. |
| | `findPoolSummaries(RequestStatus, Pageable)` | `LEFT JOIN` on the score is **required**: an inner join would hide the unscored rows the owner most needs to act on. No `ORDER BY` — ordering arrives with the `Pageable`. |
| | `countPoolSummaries(RequestStatus)` | |
| `WorkflowRepository` | `existsByRequestId(Long)` | |
| | `findByIdForUpdate(Long)` | `PESSIMISTIC_WRITE`. The lock behind `claim`. |
| | `findAssignedTo(Long, WorkflowStatus, Pageable)` | |
| | `countAssignedTo(Long, WorkflowStatus)` | |
| | `findUnclaimed(Pageable)` | Status is fixed, not a parameter — unclaimed work is only ever in `BACKLOG`. |
| | `countUnclaimed()` | |
| `UserRepository` | `findByEmail(String)`, `existsByEmail(String)` | |
| | `findBySetupToken(String)` | Holding the code is the proof of identity, and it works once. |
| | `findForAdmin(Role, String, Pageable)` | Both filters optional; `awaitingSetup` derived from the token. |
| | `countForAdmin(Role, String)` | |
| | `countByRoleAndActiveTrue(Role)` | Behind the last-administrator guard. |
| | `findByRoleAndActiveTrueOrderByNameSurname(Role)` | |
| `PrioritizationRepository` | `findByRequestId(Long)` | Absence of a row **is** the unscored state. |
| `RequestStatusHistoryRepository` | `findByRequestIdOrderByChangedAtDesc(Long)` | |
| | `findCustomerTimeline(Long)` | Filtered to the five request states, oldest first — a timeline is read forwards. |
| `NotificationRepository` | `findRecentFor(Long, Pageable)` | Newest first — a notice list is read backwards. |
| | `countByRecipientIdAndReadFalse(Long)` | |
| | `markAllRead(Long)` | One bulk `UPDATE`. |
| | `findByRelatedRequestId(Long)` | Used only by tests removing what they wrote. |

The audit table is **append-only**. Nothing in the application updates or
deletes a row in it, because an audit trail that can be modified is not an audit
trail.

---

## 10. Related Documents

| Document | Contents |
|---|---|
| [Roles & Use Cases](01-roles-and-use-cases.md) | Actors, permission matrix, 16 use-case scenarios |
| [State Diagrams](02-state-diagrams.md) | Both state machines, full transition matrices |
| [UI Design](03-ui-design.md) | Screen-to-component mapping, routing, UI conventions |
| [ERD](04-erd.md) | Schema, relationships, indexing decisions |
