# UI Design and Component Mapping

> Request Management System
> This document maps every screen in the system to concrete Vaadin components, routes, and validation rules. It is the direct reference for the implementation phase.

---

## 1. Shared Layout and Components

### 1.1 Application Shell

Every authenticated screen renders inside a single `AppLayout`:

```
MainLayout (implements RouterLayout)
├── Navbar (top)
│   ├── DrawerToggle
│   ├── H1 (current view title)
│   └── HorizontalLayout (right side)
│       ├── NotificationBell (custom component)
│       ├── ThemeToggle (custom component)
│       ├── LanguageSelect (Select: TR / EN)
│       └── MenuBar (user menu: Profile, Logout)
└── Drawer (side)
    └── SideNav (role-dependent items)
```

`MainLayout` is the parent layout for all views except the authentication screens, which render standalone.

### 1.2 Navigation Items by Role

The `SideNav` renders a different item set depending on the authenticated user's role:

| Role | Navigation items |
|---|---|
| Customer | New Request, My Requests |
| Product Owner | Prioritization Pool |
| Developer | My Tasks, Available Tasks |
| Admin | Users |

Every role also reaches Profile and Sign out from the navbar.

Navigation items are not merely hidden for unauthorized roles — the routes themselves are guarded, and the services behind them again (see §7.4). Hiding a link is a usability decision, not a security measure.

### 1.3 Reusable Components

| Component | Purpose | Used in |
|---|---|---|
| `StatusBadge` | A coloured `Span` taking a label and a tone | Everywhere a state is shown |
| `CustomerStatusPresentation` | Request status in the customer's words | Customer list and detail |
| `PoStatusPresentation` | Raw status and the score band | PO pool |
| `WorkflowStatusPresentation` | Stage names and what a move means | Developer board |
| `DeadlinePresentation` | Days remaining, colour, sort key | Developer screens |
| `UserStatePresentation` | Role and account state badges | Admin screens |
| `ConfirmDialog` | Confirmation for irreversible actions | DONE, rejection, role change, deactivation |

**`StatusBadge` takes its label rather than deriving it,** because the same state reads differently per role: a customer sees "In progress" where a product owner sees `IN_WORKFLOW`. The presentation classes hold the vocabularies, one per audience.

**Badges use literal colours rather than Lumo's custom properties.** The variables resolve against a stylesheet this application does not import and came out blank.

### 1.4 Feedback Convention

| Situation | Component | Theme variant | Duration |
|---|---|---|---|
| Successful action | `Notification` | `SUCCESS` | 3 s |
| Validation error | Inline field error | — | Until corrected |
| Server/business error | `Notification` | `ERROR` | 5 s |
| Irreversible action | `ConfirmDialog` | — | Until dismissed |

Notifications appear bottom-end. Inline field errors are always preferred over notifications for validation problems, because they point at the offending field.

---

## 2. Routing Map

| Route | View | Allowed roles | Layout |
|---|---|---|---|
| `/login` | `LoginView` | Anonymous | Standalone |
| `/setup` | `AccountSetupView` | Anonymous | Standalone |
| `/recover` | `PasswordRecoveryView` | Anonymous | Standalone |
| `/` | `RootRedirectView` | Authenticated | — |
| `/requests/new` | `NewRequestView` | Customer | Main |
| `/requests/my` | `MyRequestsView` | Customer | Main |
| `/profile` | `ProfileView` | Any authenticated | Main |
| `/po/pool` | `PrioritizationPoolView` | PO | Main |
| `/po/prioritize?requestId=` | `PrioritizationFormView` | PO | Main |
| `/dev/tasks` | `MyTasksView` | Developer | Main |
| `/dev/available` | `AvailableTasksView` | Developer | Main |
| `/admin/users` | `UserManagementView` | Admin | Main |

**Root redirect:** `/` routes each role to its primary screen — Customer to `/requests/my`, PO to `/po/pool`, Developer to `/dev/tasks`, Admin to `/admin/users`.

**The scoring form takes a query parameter rather than a path segment.** A view instance is reused across navigations, so the id is read on entry rather than held as a field, and a query parameter is what `BeforeEnterEvent` hands over without further routing setup.

---

## 3. Customer Screens

### 3.1 New Request — `/requests/new`

```
NewRequestView
└── VerticalLayout
    ├── H2 ("Report a new support / development request")
    ├── FormLayout (1 column)
    │   ├── TextField        → title
    │   └── TextArea         → description
    └── HorizontalLayout
        ├── Button (primary) → "Submit Request"
        └── Button (tertiary)→ "Clear"
```

**Field specification:**

| Field | Component | Constraints | Placeholder |
|---|---|---|---|
| Title | `TextField` | Required, 5–200 chars, `setMaxLength(200)` | "e.g. Credit card error on the payment screen…" |
| Description | `TextArea` | Required, 20–4000 chars, min height 200px | "Describe the problem or the feature you would like added…" |

**Behaviour:**
- A character counter is shown on both fields (`setHelperText` updated on value change) so the limit is visible before it is hit
- The submit button is disabled until the binder reports a valid state
- On success: `Notification` with `SUCCESS` variant, form is cleared, user is navigated to `/requests/my`
- Binding uses `Binder<RequestCreateDto>` with bean validation annotations as the single source of truth — no duplicated validation logic in the view

**Why a separate DTO:** the view binds to a create-specific DTO, not the entity. The entity carries fields the customer must never set (`status`, `customerId`), and binding directly to it would make it possible to submit them.

### 3.2 My Requests — `/requests/my`

```
MyRequestsView
└── VerticalLayout
    ├── HorizontalLayout (toolbar)
    │   ├── TextField (search by title, LAZY value change mode)
    │   └── Select (filter by status, includes "All")
    ├── Grid<CustomerRequestDto>
    │   ├── Column: title
    │   ├── Column: createdAt (formatted)
    │   ├── Column: status → StatusBadge
    │   └── Column: details → Button (opens Dialog)
    └── EmptyState (when no requests exist)
```

**Grid columns display the customer-facing status text**, never the raw enum:

| Stored status | Displayed text | Badge theme |
|---|---|---|
| `NEW` | Your request has been received | `contrast` |
| `PRIORITIZED` | Your request is under evaluation | `primary` |
| `IN_WORKFLOW` | Your request is being worked on | `primary` |
| `CLOSED` | Your request has been completed | `success` |
| `REJECTED` | Your request was not taken forward | `error` |

**Detail dialog** shows the full description and, for `REJECTED` requests, the rejection reason in a highlighted block. The dialog contains no score, no workflow stage, and no assigned developer.

**Empty state:** icon, the message "You haven't submitted any requests yet", and a primary button linking to `/requests/new`.

**Data binding:** `grid.setItemsPageable(service::listMyRequests, service::countMyRequests)` — the service resolves the customer from the session, never from a view parameter.

### 3.3 Profile — `/profile`

Shared by all roles.

```
ProfileView
└── VerticalLayout
    ├── FormLayout (personal details)
    │   ├── TextField (full name)
    │   ├── EmailField (email)
    │   ├── TextField (role, read-only)
    │   └── Button → "Save changes"
    └── Details ("Change password", collapsed)
        └── FormLayout
            ├── PasswordField (current password)
            ├── PasswordField (new password)
            ├── PasswordField (confirm new password)
            └── Button → "Change password"
```

**The role is shown and locked rather than left out.** Knowing what you are is useful, and a visibly locked field says who decides it better than an absent one would. It is also not a parameter the update accepts: a field that never arrives cannot be set by someone editing the request on its way to the server.

**Changing a password is a separate act from correcting a name,** so it has its own collapsed section and its own button. Folded into one save, the form would have to explain which half of it failed.

**The current password is required** even though the session already proves who is asking. A session left open on a shared machine proves the machine, not the person, and this is the one place that difference matters.

Reached from a Profile button in the navbar, beside Sign out.

---

## 4. Product Owner Screens

### 4.1 Prioritization Pool — `/po/pool`

This is the primary PO screen, derived from wireframe *Ekran 2*.

```
PrioritizationPoolView
└── VerticalLayout
    ├── H2 ("Customer requests awaiting prioritization")
    ├── Paragraph (explains score-based ordering)
    ├── HorizontalLayout (toolbar)
    │   ├── TextField (search: title or customer)
    │   ├── Select (status filter)
    │   └── Checkbox ("Show unscored only")
    └── Grid<RequestSummaryDto>
        ├── Column: requestId          (sortable)
        ├── Column: customerName       (sortable)
        ├── Column: title              (sortable, flex-grow)
        ├── Column: priorityScore      (sortable) → ScoreBadge
        └── Column: actions            → dynamic Button
```

**Score badge rendering:**

| Score range | Label | Badge theme |
|---|---|---|
| — (no record) | Not Assigned | `contrast` |
| 1–6 | Low | `success` |
| 7–15 | Medium | `primary` |
| 16–25 | Critical | `error` |

**Dynamic action column** — the button rendered depends on the request's status:

| Request status | Rendered controls |
|---|---|
| `NEW` | "Prioritize" (primary), "Reject" (error, tertiary) |
| `PRIORITIZED` | "Convert to Workflow" (primary), "Edit" (tertiary), "Reject" (error, tertiary) |
| `IN_WORKFLOW` | None — read-only row |

**Sorting:** the grid must declare its sortable columns explicitly (`setSortableColumns(...)`), and sorting is resolved in the backend through the `Pageable`. Without the explicit declaration Vaadin renders every column as sortable while nothing actually happens on click.

**Ordering:** default sort is priority score descending. Requests with no prioritization record sort last regardless of direction, so the pool always surfaces actionable high-priority items at the top.

**Data binding:** `setItemsPageable(service::listPool, service::countPool)` — the count callback is required, otherwise the scrollbar recalculates continuously and the user cannot jump to the end.

### 4.2 Prioritization Form — `/po/prioritize/:requestId`

Derived from wireframe *Ekran 3*.

```
PrioritizationFormView (implements BeforeEnterObserver)
└── VerticalLayout
    ├── H2 ("Request evaluation & prioritization — #{id}")
    ├── Div (read-only request summary: title, customer, description)
    └── HorizontalLayout
        ├── FormLayout (left, flex 2)
        │   ├── Select<ImpactLevel>   → "Business Impact"
        │   └── Select<UrgencyLevel>  → "Urgency"
        └── VerticalLayout (right, flex 1 — score card)
            ├── Span ("CALCULATED SCORE")
            ├── H1 (live score value)
            ├── Span (formula: "3 (Impact) × 4 (Urgency)")
            ├── ScoreBadge (Low / Medium / Critical)
            └── Button (primary) → "Save Values"
```

**Select options** render the enum's descriptive label, not the bare number:

| Impact | Label |
|---|---|
| 1 | Cosmetic — no functional effect |
| 2 | Minor — small inconvenience |
| 3 | Moderate — business processes with a workaround |
| 4 | Major — business processes with no workaround |
| 5 | Critical — core business operations blocked |

| Urgency | Label |
|---|---|
| 1 | Backlog — no defined timeline |
| 2 | Long term — a future release |
| 3 | Medium term — next sprint |
| 4 | Short term — must land in the active sprint |
| 5 | Immediate — requires intervention today |

**Live score preview:** both selects register a value change listener that recomputes `impact × urgency` and updates the score card. This is a display-only calculation — the authoritative score is computed by the database when the record is saved. The preview must never be sent to the server as the score.

**Entry guard (`BeforeEnterObserver`):** if the request is already `IN_WORKFLOW`, `CLOSED`, or `REJECTED`, the view rejects entry and forwards back to the pool with an error notification. Guarding on entry prevents a stale link or a manually typed URL from opening a form whose submission would be rejected anyway.

**Edit mode:** when the request already has a prioritization record, both selects are pre-populated and the save is processed as an update.

### 4.3 Rejection Flow

Rejection is not a separate view — it is a `Dialog` opened from the pool grid:

```
RejectRequestDialog
└── VerticalLayout
    ├── Paragraph (warning: this action cannot be undone)
    ├── TextArea → rejection reason (required, 10–500 chars)
    └── HorizontalLayout
        ├── Button (error, primary) → "Reject Request"
        └── Button (tertiary)       → "Cancel"
```

The reason is mandatory and is shown to the customer, so the helper text states this explicitly: *"This reason will be visible to the customer."* Making the visibility explicit changes how the reason is written.

**There is no assignment screen.** Conversion leaves a task unclaimed and developers pull work from the available list, so there is nothing for an owner to assign; a screen for assigning would exist to contradict that.

## 5. Developer Screens

### 5.1 My Tasks — `/dev/tasks`

```
MyTasksView
└── VerticalLayout
    ├── H2 ("My tasks")
    ├── Span (overdue banner, hidden when nothing is late)
    ├── Tabs (All | Backlog | In Progress | Testing | Done)
    └── Grid<TaskSummaryDto>
        ├── Column: requestTitle (flex-grow)
        ├── Column: priorityScore → score badge
        ├── Column: deadline      → deadline badge
        ├── Column: workflowStatus → stage badge
        └── Column: actions → transition buttons
```

**Transition buttons come from the enum.** The view asks `WorkflowStatus.allowedTransitions()` and renders one button per legal target, rather than listing them per stage. When the rules change the board follows without being edited, and a stage with nowhere to go renders nothing.

| Current stage | Rendered buttons |
|---|---|
| `BACKLOG` | "Start Work" → `IN_PROGRESS` |
| `IN_PROGRESS` | "Ready for Testing" → `TESTING` |
| `TESTING` | "Mark as Done" → `DONE` (confirm), "Test Failed" → `IN_PROGRESS` |
| `DONE` | None |

**The button says what the move means, not what the target is called.** "Test Failed" reads as a decision where "In Progress" reads as a destination.

**Only `DONE` asks first.** It is final and closes the customer's request with it. Sending a task back for rework is reversible, and confirming everything trains people to confirm without reading.

**The deadline badge carries the days remaining, and the colour carries the urgency.** A date alone says little at a glance; what a developer scanning a list needs is which rows are late and which are about to be.

| State | Reads | Tone |
|---|---|---|
| Past the deadline, not finished | `07.08.2026 (10d late)` | Red |
| Within two days | `22.08.2026 (2d left)` | Amber |
| Further out | `05.09.2026 (14d left)` | Neutral |
| Finished | `13.05.2026` | Green |
| No deadline | `No deadline` | Neutral |

A finished task stops being overdue. Delivered late is late and the report says so, but it is not still running out of time.

**The banner counts overdue work across every stage,** not the tab in view: a task running late in a tab nobody has open is exactly the one worth saying out loud.

**An overdue task keeps every button it had.** Running late is a reason to finish something, not a reason to be unable to.

**Sorted by deadline, soonest first, with undated tasks last.** A task with no deadline is not urgent, it is unmeasured, and pushing it to the bottom keeps it from displacing work that has a date to meet.

### 5.2 Available Tasks — `/dev/available`

```
AvailableTasksView
└── VerticalLayout
    ├── H2 ("Unassigned tasks")
    ├── Paragraph (claiming assigns it; the deadline is already running)
    └── Grid<TaskSummaryDto>
        ├── Column: requestTitle
        ├── Column: priorityScore → score badge
        ├── Column: deadline      → deadline badge
        ├── Column: createdAt     ("Waiting since")
        └── Column: actions → Button ("Claim")
```

**The deadline is here as well as on the board,** because it is part of what a developer agrees to by claiming: one of these may be a week away and another already late.

**Two developers can claim at the same moment.** The service reads the row under a lock, so the second one finds it taken and is told so; without the lock both would pass the check and the second write would silently replace the first, leaving the loser working on something the board says belongs to someone else.

---

## 6. Admin Screens

No wireframe exists for these; the layouts below follow the conventions established by the PO screens.

### 6.1 User Management — `/admin/users`

```
UserManagementView
└── VerticalLayout
    ├── HorizontalLayout (header)
    │   ├── H2 ("Users")
    │   └── Button (primary) → "New User"
    ├── HorizontalLayout (toolbar)
    │   ├── TextField (search: name or email, LAZY)
    │   └── Select (role filter, "All roles")
    └── Grid<AdminUserDto>
        ├── Column: name → Button (opens the detail dialog)
        ├── Column: email
        ├── Column: role → StatusBadge
        ├── Column: state → StatusBadge set
        ├── Column: createdAt
        └── Column: actions → MenuBar (overflow menu)
```

**State is a set of badges, not one status.** An account can be inactive, locked, awaiting setup, or none of those, and the states are independent: locked is what the system did after failed recovery attempts, inactive is what an administrator decided, awaiting setup is an account nobody has claimed yet. Rendering them as alternatives would suggest a user can only be one at a time.

**Actions menu** per row, as a `MenuBar` with an overflow icon so the column stays narrow. Most of these are rare, and giving each its own button would make the common ones harder to find.

| Action | Availability | Confirmation |
|---|---|---|
| Change role | Always | Yes — the person finds out by losing access |
| Issue a new setup code | Awaiting setup | Yes |
| Reset account access | Already set up | Yes — discards password and question |
| Unlock | Locked accounts | Yes |
| Deactivate | Active accounts | Yes |
| Reactivate | Inactive accounts | No |

**Unlocking clears only the lock.** Being locked out of recovery says nothing about whether the owner still knows their password; someone who has forgotten needs a setup code, which is a separate decision an administrator makes on purpose.

**The last active administrator cannot be demoted or deactivated.** An account system with nobody able to administer it cannot be repaired from inside itself. The count looks at the others, so acting on a colleague is allowed where acting on yourself as the last one is not.

### 6.2 New User Dialog

```
CreateUserDialog
└── FormLayout (1 column)
    ├── TextField  → full name
    ├── EmailField → email
    └── Select     → role
└── Paragraph (explains the setup code that follows)
└── Footer: Cancel · "Create account"
```

**Three fields: who the person is and what they may do.** Nothing about how they will prove the account is theirs.

**No password and no security question.** An administrator who chose the password would hold the account open indefinitely, and one who also chose the question would hold both doors. What comes back instead is a one-time code, and the person who uses it chooses both.

### 6.3 Setup Code Dialog

Shown once, straight after creation or after a code is reissued.

```
SetupCodeDialog (not dismissable by Esc or outside click)
├── Paragraph  → who to give it to
├── Span       → the code, monospace, grouped in fours
├── Paragraph  → expiry date
├── Paragraph  → warning that it is shown only once
└── Footer: "I have written it down"
```

Only the code itself is stored, and no screen can recover it: an administrator who closes this dialog too early has to issue a new one, which is cheaper than a screen that can show any account's code on demand.

### 6.4 User Detail Dialog

Opened from the name in the list.

```
UserDetailDialog (read-only)
├── Account   → email, role, state badges, joined date
├── Security  → security question by name, failed recovery attempts
└── Activity  → completed work, turnaround, deadline record
```

**Read-only on purpose.** Everything that changes an account is already in the row's action menu, and offering the same thing twice invites the two to disagree about what confirmation each needs.

**The security answer is never shown.** Only its hash is stored, and an administrator able to read the answer would make the question worthless as a way of proving who someone is. For an account awaiting setup the section says so rather than showing empty fields, because the question has not been chosen yet.

**The activity section answers what one person has been doing.** The audit trail already records every transition with its actor, so the account view reads the same rows the request timeline does — asked about a user rather than about a request.

---

## 7. Authentication Screens

There is no registration. This is an internal system: users are known people whose role is a decision, and someone signing themselves up could neither pick their own role nor prove they are a customer. An administrator opens the account instead, which in a real deployment would be an identity provider doing the same thing.

### 7.1 Login — `/login`

```
LoginView
└── VerticalLayout (centered)
    ├── H1 (application name)
    ├── LoginForm
    ├── Anchor → /recover  ("Forgotten your password?")
    └── Anchor → /setup    ("Have a setup code?")
```

**Error messages are deliberately uniform.** Wrong password, unknown email, locked account and an account still awaiting setup all produce the same message: *"Invalid credentials, or this account is unavailable."* Distinguishing them would let an attacker enumerate valid accounts and discover which are locked.

**Vaadin's own forgot-password button is hidden.** It raises an event rather than navigating, so it would need a listener to do what the link below already does, and two of them on one screen is one too many.

### 7.2 Account Setup — `/setup`

Reached with a one-time code an administrator hands over. This is where an account gets its credentials for the first time.

```
AccountSetupView
└── VerticalLayout (centered, max 480px)
    ├── H2 + explanatory Paragraph
    └── FormLayout
        ├── TextField        → setup code
        ├── PasswordField    → password
        ├── PasswordField    → confirm password
        ├── Select           → security question
        └── TextField        → answer
    └── Button (primary)     → "Set up account"
```

**Everything arrives at once.** The password, the question and the answer are supplied together and the code is destroyed in the same transaction, so there is no moment where both the code and the password open the account.

**Dashes and capitals are ignored.** The code is shown grouped in fours because that helps someone read it aloud; requiring the grouping back would make the formatting one more thing to get wrong.

**The security question is chosen here, not by whoever opened the account.** A question whose answer an administrator already knows proves nothing about who is asking.

### 7.3 Password Recovery — `/recover`

Two steps in one view, with the second revealed rather than routed to.

```
Step 1
└── EmailField + Button ("Continue")

Step 2 (revealed)
├── Paragraph        → the security question, in full
├── TextField        → answer
├── PasswordField    → new password
├── PasswordField    → confirm new password
└── Button (primary) → "Set new password"
```

**An unknown address still gets a question.** Saying "no such account" would turn this form into a way of testing which addresses are registered. The question shown for an unknown address is derived from the address itself, so the same one always produces the same question — one that changed between attempts would give the pretence away.

**The remaining attempt count is not shown.** A count helps whoever is guessing far more than whoever forgot, who will get it in the first two attempts or not at all.

**Three wrong answers lock the account,** and an administrator clears it. The point of a limit is that getting past it takes someone else.

### 7.4 Route Guarding

Two mechanisms, applied together.

**Role-based access** is declared per view with `@RolesAllowed`, backed by Vaadin's Spring Security integration, so a new view cannot be left unprotected by an omission in a list of URL patterns somewhere else.

**Method security** repeats the check at the service layer with `@PreAuthorize`. A view that forgets its annotation is one mistake; a service anyone reaching it can call is a hole that outlives the screen in front of it.

`MainLayout` itself is `@PermitAll`. It carries no data, and restricting it would block every role whose views live inside it.

**Accounts awaiting setup cannot sign in.** `AppUserDetails.isEnabled()` reports false while a setup token is present, which puts the refusal where Spring already looks rather than leaving a null password hash to fail somewhere less predictable.

### 7.5 Password Rules

| Rule | Value |
|---|---|
| Minimum length | 8 characters |
| Must contain | at least one letter and one digit |
| Confirmation | must match exactly |

The rule lives in one class used by every screen that sets a password, because a rule enforced in three places is a rule that will eventually differ between them.

Length and a mix of characters, and nothing else. Rules that demand symbols and forbid repeats push people towards writing passwords down, which trades a guessable password for a discoverable one.

---

## 8. Cross-Cutting UI Rules

### 8.1 Grid Conventions

Applied to every grid in the application:

- Data is bound with `setItemsPageable(fetch, count)` — the count callback is always supplied
- Sortable columns are declared explicitly with `setSortableColumns(...)`
- Sorting and filtering resolve on the server through the `Pageable`; no in-memory sorting of a fully loaded list
- Filter fields use `ValueChangeMode.LAZY` so a query fires after a typing pause rather than per keystroke
- Vaadin's grid uses infinite scrolling rather than numbered pages; no custom pagination controls are built

### 8.2 Empty and Loading States

Every grid has a defined empty state — an icon, an explanatory message, and where useful a primary action. A blank grid with no explanation reads as a failure rather than an absence of data.

Long-running operations disable their trigger button and show its loading state rather than leaving the interface apparently idle.

### 8.3 Confirmation Policy

A `ConfirmDialog` is required for exactly those actions that cannot be undone:

| Action | Confirmation | Reason |
|---|---|---|
| Mark task as `DONE` | Yes | Final stage; also closes the request |
| Reject request | Yes | Dead end, and the customer is told why |
| Change user role | Yes | The person finds out by losing access |
| Deactivate user | Yes | Revokes access |
| Reset account access | Yes | Discards the password and the security question |
| Unlock account | Yes | |
| Test failed (`TESTING` → `IN_PROGRESS`) | No | Reversible |
| Claim task | No | Reversible by reassignment |
| Reactivate user | No | Restores access rather than removing it |

The policy is derived from the state machine rather than decided per screen: a transition into a final state is confirmed, and a reversible one is not. Confirming everything trains people to confirm without reading, which costs more than it saves.

### 8.4 Responsive Behaviour

`FormLayout` uses responsive steps so forms collapse from two columns to one below roughly 640px. The `AppLayout` drawer switches to overlay mode on narrow viewports automatically. Grids with many columns hide secondary columns below a breakpoint rather than compressing every column into illegibility.
