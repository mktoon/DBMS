# CareDesk — Hospital Appointment Manager

A staff-facing Java web application built from my hospital database coursework. **Spring Boot · Thymeleaf · JDBC · MySQL**

## Version 1

| Module | Working features |
| --- | --- |
| Patients | Register, search by name or ID, edit details, view appointment history |
| Physicians | Browse the directory, filter by department, open a physician's daily schedule |
| Appointments | Book, reschedule, cancel, and complete visits; prevent patient and physician overlaps |
| Reports | Daily dashboard and date-range totals by department, physician, and status |

The app includes staff sign-in, protected forms, local-time display, a restricted database account, and a responsive web interface. This is a portfolio demo for fictional data. See the [application guide](app/README.md) for architecture and limitations, and the [database guide](database/v1/README.md) for SQL design.

## Quick start

Requires Git and Docker Compose with the `--wait` option. Java and Maven run inside the app's build container.

```bash
git clone https://github.com/mktoon/DBMS.git
cd DBMS
cp .env.example .env
# Edit .env: set three different passwords and your clinic time zone.
docker compose up -d --wait database
docker compose exec -T database sh /opt/hospital/provision-app.sh
# Optional: load fictional patients, physicians, and appointments ONCE.
docker compose exec -T database sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < database/v1/seed.sql
docker compose up -d --build app
docker compose logs -f app
```

Open **http://localhost:8080** after the app starts. Sign in with `HOSPITAL_USERNAME` and `HOSPITAL_PASSWORD` from `.env`. Stop following logs with Ctrl+C; the containers keep running.

Already ran the database milestone? Run `git pull`, add the new variables from `.env.example` to your existing `.env`, then run the database, provisioning, and app commands above. **Skip the seed if it was already loaded.** Existing V1 data stays in the same volume. No new schema migration is required.

Initialization creates `hospital_v1` only in a new volume. `docker compose down` stops the app and database while retaining records. The provisioning script creates or updates the dedicated `hospital_app` database account to match `.env`.

The first app build downloads dependencies and can take several minutes. If port 8080 is occupied, change only the host-side port in `compose.yaml`, for example `127.0.0.1:8081:8080`.

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

- [Database integration](.github/workflows/database.yml): 18 tests against MySQL 8.4, including concurrent bookings.
- [Application integration](.github/workflows/application.yml): Java date/time tests and HTTP tests of the packaged app against MySQL 8.4 using the restricted role.
- Run instructions: [database tests](database/v1/README.md#integration-tests), [application tests](app/README.md#testing).

**Author:** Micah Too
