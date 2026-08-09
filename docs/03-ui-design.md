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
| Product Owner | Prioritization Pool, Workflows, Analytics |
| Developer | My Tasks, Available Tasks |
| Admin | Users, System Overview, Analytics |

Navigation items are not merely hidden for unauthorized roles — the routes themselves are guarded (see §7.2). Hiding a link is a usability decision, not a security measure.

### 1.3 Reusable Components

| Component | Purpose | Used in |
|---|---|---|
| `StatusBadge` | Renders a status as a coloured `Span` with theme variant | Request lists, task board |
| `ScoreBadge` | Renders priority score with Low/Medium/Critical colour | PO pool, developer task list |
| `ConfirmDialog` | Confirmation for irreversible actions | DONE transition, rejection |
| `NotificationBell` | Unread counter + dropdown of recent notifications | Navbar (all roles) |
| `ThemeToggle` | Switches Lumo light/dark, persists preference | Navbar (all roles) |
| `EmptyState` | Icon + message + optional action button | Any empty grid |

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
| `/forgot-password` | `ForgotPasswordView` | Anonymous | Standalone |
| `/change-password` | `ForcedPasswordChangeView` | Any authenticated with flag set | Standalone |
| `/` | Redirect by role | Authenticated | — |
| `/requests/new` | `NewRequestView` | Customer | Main |
| `/requests/my` | `MyRequestsView` | Customer | Main |
| `/profile` | `ProfileView` | Any authenticated | Main |
| `/po/pool` | `PrioritizationPoolView` | PO | Main |
| `/po/prioritize/:requestId` | `PrioritizationFormView` | PO | Main |
| `/po/workflows` | `WorkflowOverviewView` | PO | Main |
| `/po/analytics` | `AnalyticsView` | PO, Admin | Main |
| `/dev/tasks` | `MyTasksView` | Developer | Main |
| `/dev/available` | `AvailableTasksView` | Developer | Main |
| `/admin/users` | `UserManagementView` | Admin | Main |
| `/admin/overview` | `SystemOverviewView` | Admin | Main |

**Root redirect:** `/` routes each role to its primary screen — Customer to `/requests/my`, PO to `/po/pool`, Developer to `/dev/tasks`, Admin to `/admin/users`.

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

The role field is rendered read-only with a helper text explaining that role changes are handled by an administrator. Password change is deliberately placed in a collapsed `Details` section — it is a separate operation with a separate endpoint, and separating it visually reinforces that.

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

### 4.4 Workflow Overview — `/po/workflows`

```
WorkflowOverviewView
└── VerticalLayout
    ├── H2 ("Active workflows")
    ├── HorizontalLayout (status filter tabs: All / Backlog / In Progress / Testing / Done)
    └── Grid<WorkflowSummaryDto>
        ├── Column: taskId
        ├── Column: requestTitle (flex-grow)
        ├── Column: priorityScore → ScoreBadge
        ├── Column: workflowStatus → StatusBadge
        ├── Column: developerName ("Unassigned" when null)
        └── Column: actions → Button ("Assign Developer")
```

**Assign developer** opens a `Dialog` containing a `ComboBox<UserDto>` populated only with active users whose role is `DEVELOPER`. The combo box uses lazy loading with a filter callback so the developer list is queried on the server as the PO types.

### 4.5 Analytics — `/po/analytics`

```
AnalyticsView
└── VerticalLayout
    ├── HorizontalLayout (KPI cards)
    │   ├── Card: total open requests
    │   ├── Card: average resolution time
    │   ├── Card: requests this month
    │   └── Card: test rework rate
    ├── HorizontalLayout (charts row 1)
    │   ├── ChartWrapper (bar)  → monthly request volume
    │   └── ChartWrapper (pie)  → status distribution
    ├── HorizontalLayout (charts row 2)
    │   ├── ChartWrapper (bar)  → developer performance
    │   └── Grid              → top 5 requesting customers
    └── Button → "Export to Excel"
```

**Chart rendering:** Vaadin Charts is a commercial component and is not used. Charts are rendered through a custom `ChartWrapper` component that wraps Chart.js via the Element API and `@JavaScript`. The wrapper exposes a small typed API (`setLabels`, `setValues`, `setType`) so views never touch JavaScript directly.

**Layout:** every chart row uses `FlexLayout` with wrapping enabled so the dashboard degrades to a single column on narrow viewports without a commercial dashboard layout component.

---

## 5. Developer Screens

### 5.1 My Tasks — `/dev/tasks`

```
MyTasksView
└── VerticalLayout
    ├── H2 ("My tasks")
    ├── Tabs (Backlog | In Progress | Testing | Done)
    └── Grid<TaskDto>
        ├── Column: requestTitle (flex-grow)
        ├── Column: priorityScore → ScoreBadge
        ├── Column: workflowStatus → StatusBadge
        ├── Column: assignedAt
        └── Column: actions → transition buttons
```

**Transition buttons** are rendered from the enum's allowed targets rather than hard-coded per status. The view asks `WorkflowStatus.allowedTransitions()` and renders one button per legal target. When the transition rules change, the UI follows automatically — the rules live in exactly one place.

| Current status | Rendered buttons |
|---|---|
| `BACKLOG` | "Start Work" → `IN_PROGRESS` |
| `IN_PROGRESS` | "Ready for Testing" → `TESTING` |
| `TESTING` | "Mark as Done" → `DONE` (confirm), "Test Failed" → `IN_PROGRESS` |
| `DONE` | None |

**Confirmation:** the `DONE` transition opens a `ConfirmDialog` stating that the task cannot be reopened and that the customer's request will be closed automatically. Reversible transitions are executed immediately without confirmation.

**Sorting:** default sort is priority score descending, so the highest-impact work surfaces first.

### 5.2 Available Tasks — `/dev/available`

```
AvailableTasksView
└── VerticalLayout
    ├── H2 ("Unassigned tasks")
    ├── Paragraph (explains that claiming assigns the task)
    ├── Grid<TaskDto>
    │   ├── Column: requestTitle
    │   ├── Column: priorityScore → ScoreBadge
    │   ├── Column: createdAt
    │   └── Column: actions → Button ("Claim")
    └── EmptyState ("No unassigned tasks right now")
```

**Concurrency handling in the UI:** claiming a task can fail if another developer claimed it first. The view catches this conflict, shows an error notification explaining that the task was taken, and refreshes the grid — it does not leave a stale row visible with a dead button.

---

## 6. Admin Screens

No wireframe exists for these; the layouts below follow the conventions established by the PO screens.

### 6.1 User Management — `/admin/users`

```
UserManagementView
└── VerticalLayout
    ├── HorizontalLayout (header)
    │   ├── H2 ("User management")
    │   └── Button (primary) → "New User"
    ├── HorizontalLayout (toolbar)
    │   ├── TextField (search: name or email)
    │   ├── Select (role filter, includes "All")
    │   └── Select (state filter: All / Active / Inactive / Locked)
    └── Grid<AdminUserDto>
        ├── Column: fullName (sortable)
        ├── Column: email (sortable)
        ├── Column: role → StatusBadge
        ├── Column: state → StatusBadge (Active / Inactive / Locked)
        ├── Column: createdAt
        └── Column: actions → MenuBar (overflow menu)
```

**Actions menu** per row, rendered as a `MenuBar` with an overflow icon so the column stays narrow:

| Action | Availability | Confirmation |
|---|---|---|
| Edit details | Always | No |
| Change role | Always | Yes — role changes affect access immediately |
| Deactivate | Active users | Yes |
| Reactivate | Inactive users | No |
| Unlock & reset password | Locked users | Yes |

**State column semantics:** a user can be active, inactive, or locked. Locked is a distinct state from inactive — locked means too many failed password-reset attempts, while inactive means an administrator disabled the account. They are shown with different badge themes so they are never confused.

### 6.2 User Create / Edit Dialog

```
UserFormDialog
└── FormLayout (2 columns)
    ├── TextField      → full name (required)
    ├── EmailField     → email (required, unique)
    ├── Select<Role>   → role (required)
    ├── Select<SecurityQuestion> → security question (required, create only)
    ├── TextField      → security answer (required, create only)
    └── HorizontalLayout (footer)
        ├── Button (primary) → "Save"
        └── Button (tertiary)→ "Cancel"
```

**After creation** the dialog is replaced by a result panel showing the generated temporary password with a copy-to-clipboard button, and a warning that the password is displayed only once. The user is created with `must_change_password = 1`.

**Edit mode** hides the security question fields — an administrator does not change another user's security answer. If the answer needs resetting, the unlock action handles it.

### 6.3 System Overview — `/admin/overview`

```
SystemOverviewView
└── VerticalLayout
    ├── HorizontalLayout (KPI cards)
    │   ├── Card: total users (by role breakdown)
    │   ├── Card: active vs inactive ratio
    │   ├── Card: locked accounts
    │   └── Card: total requests
    ├── H3 ("All requests")
    ├── Grid<AdminRequestDto>   → unrestricted view of every request
    └── H3 ("Recent status changes")
        └── Grid<StatusHistoryDto> → audit trail, newest first
```

The admin request grid is read-only. Admin has full visibility but no authority to advance the state machine — there are no action buttons on these rows, which makes the boundary visible in the interface itself.

---

## 7. Authentication Screens

### 7.1 Login — `/login`

```
LoginView
└── VerticalLayout (centered)
    └── LoginForm
        ├── i18n: title, username = "Email", password
        └── forgotPasswordButton → navigates to /forgot-password
```

`LoginForm` is used rather than `LoginOverlay` so the page can carry the application name and a footer without fighting the overlay's fixed structure.

**Error messages** are deliberately uniform. Wrong password, unknown email, and locked account all produce the same message: *"Invalid credentials, or this account is unavailable."* Distinguishing them would let an attacker enumerate valid accounts and discover which are locked. The exception is the inactive-account case after a successful credential check, where a distinct message is acceptable because credentials were already proven.

### 7.2 Route Guarding

Two mechanisms, applied together:

**Role-based access** is enforced with Spring Security's `@RolesAllowed` (or `@PermitAll` for public views) on the view class, backed by Vaadin's Spring Security integration. An unauthorized navigation attempt is rejected before the view is instantiated.

**Forced password change** is enforced with a `BeforeEnterObserver` registered globally: when the authenticated user has `must_change_password = 1` and the target is not `/change-password`, the navigation is rerouted. This is why the flag blocks the whole application rather than a single screen.

### 7.3 Forgot Password — `/forgot-password`

A two-step flow within a single view, using an internal step state rather than two routes:

```
Step 1
└── FormLayout
    ├── EmailField → email
    └── Button → "Continue"

Step 2
└── FormLayout
    ├── Span (the user's security question, read-only)
    ├── TextField → answer
    └── Button → "Verify"

Step 3
└── FormLayout
    ├── PasswordField → new password
    ├── PasswordField → confirm password
    └── Button → "Set new password"
```

**Attempt feedback:** the remaining attempt count is *not* shown. Telling an attacker how many guesses remain is more useful to them than to a legitimate user, who will typically succeed on the first or second try. When the limit is reached, the message states that the account is locked and directs the user to contact an administrator.

**Unknown email:** step 1 always advances to step 2 with a generic placeholder question. Revealing that an email is unregistered enables account enumeration. The verification in step 2 then simply fails.

### 7.4 Forced Password Change — `/change-password`

```
ForcedPasswordChangeView
└── VerticalLayout (centered)
    ├── Paragraph (explains why the change is required)
    ├── PasswordField → new password
    ├── PasswordField → confirm password
    └── Button → "Set password and continue"
```

Rendered standalone, outside `MainLayout` — the navigation drawer is not shown, because there is nowhere else the user may go until the password is set.

**Password rules** (applied here and in the profile screen):

| Rule | Value |
|---|---|
| Minimum length | 8 characters |
| Must contain | at least one letter and one digit |
| Confirmation | must match exactly |

Rules are validated on both the client (for immediate feedback) and the server (as the authority). The client-side check is a convenience, never a control.

---

## 8. Cross-Cutting UI Rules

### 8.1 Theme

Lumo's built-in light and dark variants are used. The toggle sets the theme attribute on the document element and persists the choice to `users.preferred_theme`, so it survives a new session on a different device. The stored preference is applied during layout construction to avoid a visible flash of the wrong theme.

### 8.2 Internationalisation

All user-facing strings resolve through Vaadin's `I18NProvider` backed by `messages_tr.properties` and `messages_en.properties`. No literal display strings appear in view code. The active locale comes from `users.preferred_language`, with the language select in the navbar updating both the session locale and the stored preference.

Status labels, enum descriptions, and validation messages are all resolved through the same mechanism — a status badge shows the translated label, never the enum name.

### 8.3 Grid Conventions

Applied to every grid in the application:

- Data is bound with `setItemsPageable(fetch, count)` — the count callback is always supplied
- Sortable columns are declared explicitly with `setSortableColumns(...)`
- Sorting and filtering resolve on the server through the `Pageable`; no in-memory sorting of a fully loaded list
- Filter fields use `ValueChangeMode.LAZY` so a query fires after a typing pause rather than per keystroke
- Vaadin's grid uses infinite scrolling rather than numbered pages; no custom pagination controls are built

### 8.4 Empty and Loading States

Every grid has a defined empty state — an icon, an explanatory message, and where useful a primary action. A blank grid with no explanation reads as a failure rather than an absence of data.

Long-running operations disable their trigger button and show its loading state rather than leaving the interface apparently idle.

### 8.5 Confirmation Policy

A `ConfirmDialog` is required for exactly those actions that cannot be undone:

| Action | Confirmation | Reason |
|---|---|---|
| Mark task as `DONE` | Yes | Final state; also closes the request |
| Reject request | Yes | Dead-end state, visible to the customer |
| Change user role | Yes | Immediately alters access |
| Deactivate user | Yes | Revokes access |
| Test failed (`TESTING` → `IN_PROGRESS`) | No | Reversible |
| Claim task | No | Reversible by reassignment |

The policy is derived from the state machine rather than decided per screen: if a transition leads to a final state, it is confirmed.

### 8.6 Responsive Behaviour

`FormLayout` uses responsive steps so forms collapse from two columns to one below roughly 640px. The `AppLayout` drawer switches to overlay mode on narrow viewports automatically. Grids with many columns hide secondary columns below a breakpoint rather than compressing every column into illegibility.
