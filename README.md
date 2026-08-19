# Request Management System

A web-based automation system for collecting customer requests, prioritizing
them by impact and urgency, and converting them into tracked development work.

> Built as a 30-day internship project.

---

## What it does

A customer raises a request in plain language. A product owner scores it by
business impact and urgency, and the database derives a priority from the two.
Scored work is scheduled into a development task with a deadline that follows
from that priority. A developer takes the task, moves it through the board, and
finishing it closes the customer's request. Every step is recorded, and the
customer can see the trail without seeing the score behind it.

Four roles, each with its own screens: **Customer**, **Product Owner**,
**Developer**, **Admin**.

---

## Tech Stack

- Java 21, Spring Boot 4.1
- Vaadin 25 (Flow)
- Oracle Database 12.1, PL/SQL

---

## Running it

### 1. What you need

- **Java 21** — `java -version` should report 21 or later
- **An Oracle schema** with privileges to create tables and sequences
- Nothing else. Maven comes with the project via the wrapper.

### 2. Clone

```bash
git clone https://github.com/yigittgulbeyaz/request-management-system.git
cd request-management-system
```

### 3. Create the schema

Run the scripts in `src/main/resources/db/` **in this order**:

| Script | What it does |
|---|---|
| `01_create_tables.sql` | Sequences, tables, constraints |
| `02_indexes.sql` | Indexes |
| `03_seed_data.sql` | Scenario data covering every status and edge case |
| `04_seed_volume.sql` | Generated volume so the reporting screens have something to report |

Every object carries a `YIGIT_` prefix, because the schema is shared with
other people's work. `99_drop_all.sql` removes all of it, in dependency order.
Development only.

The seed files are pure ASCII: Turkish characters are written as `UNISTR`
escapes so no editor's encoding can mangle them on the way in.

### 4. Point it at your database

```bash
cp src/main/resources/application-local.yaml.example \
   src/main/resources/application-local.yaml
```

Fill in the URL, username and password. The file is gitignored and must never
be committed.

### 5. Run

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Then open **http://localhost:8080**. The first start takes a minute while
Vaadin builds its frontend bundle; later ones are quick.

### 6. Sign in

| Role | Email | Password |
|---|---|---|
| Customer | `ahmet.yilmaz@teknocorp.com` | `customer123456` |
| Product Owner | `elif.kaya@company.com` | `po123456` |
| Developer | `deniz.yildirim@company.com` | `developer123456` |
| Admin | `admin@company.com` | `admin123456` |

The security answer on every seeded account is `ankara`.

One account is deliberately left unset up, the way an administrator leaves one
behind: **`serkan.bulut@teknocorp.com`** has the code **`SETUPDEMO2026`** and
no password at all. Use it at `/setup` to see how an account gets its
credentials — sign-in for it is refused until someone does.

---

## Testing

```bash
./mvnw test
```

Most of the suite runs against mocks and needs nothing. Four classes talk to
the database and will fail without a reachable schema:

| Class | What it needs a database for |
|---|---|
| `RequestRepositoryQueryCountTest` | Counting the queries a page of the pool actually costs |
| `TransactionBoundaryTest` | Reading back what a committed state change stored |
| `TransactionRollbackTest` | Observing what survives a transaction that failed part-way |
| `WorkflowConcurrencyTest` | Two threads reaching for one task at the same moment |

These four write rows and remove them afterwards. Nothing they create is
visible to a user, but they are not read-only.

---

## Documentation

| Document | Contents |
|---|---|
| [Roles & Use Cases](docs/01-roles-and-use-cases.md) | Actors, permission matrix, 18 use-case scenarios |
| [State Diagrams](docs/02-state-diagrams.md) | Request and workflow state machines, transition rules |
| [UI Design](docs/03-ui-design.md) | Screen-to-component mapping, routing, UI conventions |
| [ERD](docs/04-erd.md) | Schema, relationships, indexing decisions |

---

## Notable Decisions

**Priority score is a virtual column.** `impact * urgency` is derived by the
database, so the application sends only the two inputs and cannot disagree
with the stored score.

**Transition rules live in the enums.** A status knows which statuses it may
become, so the rule is written once. The developer board renders one button
per allowed target rather than listing them per stage, so a change to the
rules reaches the screen without it being edited.

**Customers never see the score.** The DTO returned to a customer does not
carry the field at all, rather than nulling it out on the way past. The same
applies to the progress timeline: it shows the request's own states, not the
workflow stages behind them.

**Work is pulled, not assigned.** A scheduled request waits unclaimed until a
developer takes it. Nobody hands work out, because the person doing it knows
better than anyone which of it they can pick up.

**Deadlines come from the priority band, not from a date anyone picks.** Two
days for critical work, twenty for low. Committed when the request is
scheduled and stored rather than recomputed, because a change to the formula
should not move promises already made.

**An account has a setup code or a password, never both.** An administrator
opens an account without credentials and hands over a one-time code; the
person who uses it chooses the password and the security question, and the
code dies in the same transaction. An administrator who chose either would
hold the account open indefinitely.

**List screens read through projections.** The pool spans three tables, and
walking entity associations would cost a query per row for the customer and
another for the score. A test asserts the count stays at one whether the page
holds five rows or thirty.

**Roles are enforced twice.** Views declare `@RolesAllowed` and services
declare `@PreAuthorize`. A view that forgets its annotation is one mistake; a
service anyone reaching it can call is a hole that outlives the screen in
front of it.

**Views throw rather than catch.** A rule broken in a service reaches a Vaadin
`ErrorHandler` that turns the error code into a sentence, so no screen writes
its own version of the same message. Exceptions carry an error code rather
than an HTTP status: the entities that throw them have no business knowing
about HTTP.

**State changes are atomic.** The score, the status, the trail entry and the
customer's notice are written in one transaction. A trail entry that survived
a rolled-back change would record something that never happened.

**Two developers cannot claim one task.** The row is locked for the whole of
the check and the write, so the second reader finds it taken rather than
silently replacing the first.

**Sequences rather than identity columns.** On Oracle 12.1 the current JDBC
driver cannot return a generated key to Hibernate after an insert. Sequences
are read before the insert, and leave JDBC batching available.

---

## Status

Working end to end for all four roles.

| Area | State |
|---|---|
| Customer requests | Submission, own-request listing, detail with progress timeline |
| Prioritization | Scoring, revision, rejection with a reason |
| Workflow | Conversion, claiming, stage transitions, deadlines, automatic closure |
| Accounts | Setup codes, password recovery, profile and password management |
| Administration | User list, role changes, deactivation, unlocking, account detail |
| Audit and notifications | Written with every state change |

**Currently building:** the analytics dashboard, including developer capacity
— open work per developer alongside what they have finished. Not an assignment
aid, since nobody assigns: it answers whether the people are full while the
backlog grows. Also in progress: comments on a request, and the in-app
notification centre that reads the notifications already being written.