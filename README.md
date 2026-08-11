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

## Notable Decisions

**Priority score is a virtual column.** `impact * urgency` is derived by the
database, so the application sends only the two inputs and cannot disagree
with the stored score.

**Transition rules live in the enums.** A status knows which statuses it may
become, so the rule is written once and the UI derives its buttons from the
same source.

**Customers never see the score.** The DTO returned to a customer does not
carry the field at all, rather than nulling it out on the way past.

**Sequences rather than identity columns.** On Oracle 12.1 the current JDBC
driver cannot return a generated key to Hibernate after an insert. Sequences
are read before the insert, and leave JDBC batching available.

## Status

In development. Customer request submission and listing are implemented.