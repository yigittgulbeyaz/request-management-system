# Request Management System

A web-based automation system for collecting customer requests, prioritizing
them by impact and urgency, and converting them into development workflows.

> Built as a 30-day internship project.

## Tech Stack

- Java 21, Spring Boot 4.1
- Vaadin 25 (Flow)
- Oracle Database 12.1, PL/SQL

## Documentation

| Document | Contents |
|---|---|
| [Roles & Use Cases](docs/01-roles-and-use-cases.md) | Actors, permission matrix, 18 use-case scenarios |
| [State Diagrams](docs/02-state-diagrams.md) | Request and workflow state machines, transition rules |
| [UI Design](docs/03-ui-design.md) | Screen-to-component mapping, routing, UI conventions |
| [ERD](docs/04-erd.md) | Schema, relationships, indexing decisions |

## Getting Started

### Prerequisites

- Java 21
- An Oracle schema with DDL privileges

### Database

Run the scripts in `src/main/resources/db/` in order:

| Script | Purpose |
|---|---|
| `01_create_tables.sql` | Sequences, tables, constraints |
| `02_indexes.sql` | Indexes |
| `03_seed_data.sql` | Scenario data covering every status and edge case |
| `04_seed_volume.sql` | Generated volume so the analytics screens have data |

`99_drop_all.sql` resets the schema. Development only.

All objects carry a `YIGIT_` prefix because the schema is shared.

### Configuration

Copy `src/main/resources/application-local.yaml.example` to
`application-local.yaml` and fill in your connection details. The file is
gitignored and must never be committed.

### Run

```bash
./mvnw spring-boot:run
```

Then open http://localhost:8080.

### Development credentials

Seeded accounts, listed at the top of `03_seed_data.sql`:

| Role | Email | Password |
|---|---|---|
| Customer | `ahmet.yilmaz@teknocorp.com` | `customer123456` |
| Product Owner | `elif.kaya@company.com` | `po123456` |
| Developer | `deniz.yildirim@company.com` | `developer123456` |
| Admin | `admin@company.com` | `admin123456` |

## Testing

```bash
./mvnw test
```

Most of the suite runs against mocks and needs nothing. Three classes talk to
the database and will fail without a reachable schema:

| Class | What it needs a database for |
|---|---|
| `RequestRepositoryQueryCountTest` | Counting the queries a page of the pool actually costs |
| `TransactionBoundaryTest` | Reading back what a committed state change stored |
| `TransactionRollbackTest` | Observing what survives a transaction that failed part-way |

These three write rows and remove them afterwards. Nothing they create is
visible to a user, but they are not read-only.

## Notable Decisions

**Priority score is a virtual column.** `impact * urgency` is derived by the
database, so the application sends only the two inputs and cannot disagree
with the stored score.

**Transition rules live in the enums.** A status knows which statuses it may
become, so the rule is written once. The developer board renders one button
per allowed target rather than listing them per stage, so a change to the
rules reaches the screen without it being edited.

**Customers never see the score.** The DTO returned to a customer does not
carry the field at all, rather than nulling it out on the way past.

**List screens read through projections.** The pool spans three tables, and
walking entity associations would cost a query per row for the customer and
another for the score. A test asserts the count stays at one whether the page
holds five rows or thirty.

**Views throw rather than catch.** A rule broken in a service reaches a Vaadin
`ErrorHandler` that turns the error code into a sentence, so no screen writes
its own version of the same message. Exceptions carry an error code rather
than an HTTP status: the entities that throw them have no business knowing
about HTTP.

**State changes are atomic.** The score, the status, the trail entry and the
customer's notice are written in one transaction. A trail entry that survived
a rolled-back change would record something that never happened.

**Sequences rather than identity columns.** On Oracle 12.1 the current JDBC
driver cannot return a generated key to Hibernate after an insert. Sequences
are read before the insert, and leave JDBC batching available.

## Status

Working end to end for the three business roles.

| Area | State |
|---|---|
| Customer requests | Submission, own-request listing, detail with rejection reason |
| Prioritization | Scoring, revision, rejection with a reason |
| Workflow | Conversion, claiming, stage transitions, automatic closure on completion |
| Audit and notifications | Written with every state change |
| Authentication | Database-backed sign-in, role-based routing |

Not yet built: the admin panel, password recovery, analytics, theming and
translation.