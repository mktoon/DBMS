# Hospital Management Database

MySQL and Java coursework evolving into a hospital appointment management application.

## Current milestone: Version 1 database

The new [`database/v1`](database/v1/) baseline supports patient registration, a physician directory, appointment booking/rescheduling/cancellation, and reporting. It includes MySQL constraints, transactional scheduling procedures, restricted application permissions, fictional seed data, and integration tests.

The web interface and Spring Boot backend are the next milestones. See the [V1 database guide](database/v1/README.md) for the schema, runnable examples, design decisions, and transition from the original coursework.

## Quick start

Requires Git and Docker Compose with the `--wait` option.

```bash
git clone https://github.com/mktoon/DBMS.git
cd DBMS
cp .env.example .env
# Edit .env and choose your own local database password.
docker compose up -d --wait database
docker compose exec -T database sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < database/v1/seed.sql
docker compose exec -T database sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < database/v1/reports.sql
```

Initialization creates `hospital_v1` in a new persistent Docker volume. Run the optional seed once. The baseline intentionally fails if `hospital_v1` already exists; Compose runs initialization only for a new volume. `docker compose down` stops the database and retains the volume.

## Original coursework

The original `HospitalDB` files are still available at the repository root:

| File | Coursework content |
| --- | --- |
| `hospitalDB.sql`, `projectphaser1.sql` | Original hospital schema, including procedures, medications, admissions, and nursing entities |
| `data entry` | Original sample inserts |
| `queries phase2 .sql` | Original reporting exercises |
| `phase3DB.java` | Java/JDBC console program |
| `EER diagram.mwb` | Original MySQL Workbench model |

These files use the original table and column names. The original Java console is not compatible with the V1 schema. V1 uses a separate database and does not import or modify an existing `HospitalDB`.

## Tests

[Database integration workflow](.github/workflows/database.yml) runs the V1 baseline, fixtures, and tests against MySQL 8.4. Local instructions are in the [V1 guide](database/v1/README.md#integration-tests).

**Author:** Micah Too
