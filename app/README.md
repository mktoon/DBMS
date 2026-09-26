# CareDesk application

Java 17, Spring Boot 4.1.1, Spring MVC, Thymeleaf, Spring Security, JDBC, and MySQL. Run the [root quick start](../README.md#quick-start) for the full Docker setup.

## Try the workflow

1. Sign in with the staff credentials you chose in `.env`.
2. Open **Patients → Register patient** and add fictional details.
3. From the patient record, choose **Book appointment**. Choose a physician and future start/end times.
4. Attempt another overlapping booking to see a validation message. Patient and physician conflicts are both checked.
5. Open the appointment to reschedule, cancel, or complete it after its start time. Cancellation retains history and frees the slot.
6. Use **Appointments** for a day/physician/status filter and **Reports** for a date range.

The physician directory is read-only. Demo physicians come from `database/v1/seed.sql`; managing physicians/departments through forms is outside V1.

## Architecture

Controllers bind and validate forms. `ClinicRepository` uses parameterized SQL for directories, patient operations, and reports. `SchedulingService` calls the existing MySQL procedures through JDBC with autocommit enabled, consumes all result sets/update counts, and returns selected business errors to the form. Scheduling calls are deliberately **not** wrapped in Spring `@Transactional`: the procedures own their transactions.

The app never initializes or migrates the database at startup. Setup runs separately with administrative credentials; the running application receives only the `hospital_v1_app` role. The four merged baseline migrations are unchanged.

| Path | Purpose |
| --- | --- |
| `/` | Today's patient/physician totals and appointment schedule |
| `/patients`, `/patients/new`, `/patients/{id}` | Search, register, inspect, and edit patient records |
| `/physicians` | Department-filtered physician directory |
| `/appointments`, `/appointments/new`, `/appointments/{id}` | Daily list, booking, details, rescheduling, and status actions |
| `/reports` | Date-range counts by department and physician |
| `/login` | Staff sign-in |

All mutations use POST with CSRF tokens. Successful changes redirect to their record so refreshing does not repeat the POST. Templates escape user-provided text; error pages do not expose SQL or stack traces.

## Time and reporting

- Set `HOSPITAL_TIME_ZONE` to an IANA zone such as `America/Los_Angeles` or `UTC` before starting. Invalid zones stop startup.
- Appointment inputs and display use that clinic zone. Display includes the actual UTC offset; storage and JDBC sessions use UTC.
- Nonexistent or ambiguous local inputs during daylight-saving transitions are rejected with an explanation. Choose a time outside the skipped/repeated hour.
- Date filters include the whole local day. Reports include both selected dates and accept at most 366 days. They count visits by start time and **current** status, not historical status as of the report date.
- Patient and appointment directories paginate 50 rows at a time. Patient history shows the 50 latest appointment dates. Patient booking starts from search, so there is no dropdown that silently omits patients.

## Configuration

| Variable | Default / requirement |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | Setup/Compose only; never passed to the application container |
| `MYSQL_APP_PASSWORD` | Required; password for the dedicated application database account |
| `HOSPITAL_USERNAME` | `staff` |
| `HOSPITAL_PASSWORD` | Required; 12+ characters and at most 72 UTF-8 bytes |
| `HOSPITAL_TIME_ZONE` | `UTC`; `.env.example` selects `America/Los_Angeles` |
| `DB_HOST`, `DB_PORT`, `DB_USERNAME` | `127.0.0.1`, `3307`, `hospital_app` outside Compose |
| `DB_SSL_MODE` | `PREFERRED` for local MySQL; configure certificate verification for remote deployments |
| `DB_ALLOW_PUBLIC_KEY_RETRIEVAL` | `false`; enabled only in disposable CI tests where TLS is disabled |
| `APP_BIND_ADDRESS`, `PORT` | `127.0.0.1`, `8080`; Compose overrides the bind address inside its container |

Compose publishes both services only on the local machine. Keep `.env` out of Git. Single-quote passwords containing `$` in Compose's `.env` format. After changing `MYSQL_APP_PASSWORD`, rerun provisioning and recreate the app with `docker compose up -d app`. The provisioning script manages the dedicated `hospital_app` account, clears its direct grants, and sets the V1 role; do not reuse this account for other applications or grant extra roles to it.

The single configured staff account is hashed in memory with BCrypt at startup. Staff account management, multiple staff roles, and a patient-change audit log are future work. Session expiry is 30 minutes. This demo has no production healthcare compliance claim; use fictional records. A production deployment would also need HTTPS, appropriate identity/access controls, auditing, monitoring, and backup/restore operations.

## Run without the app container

Start and provision MySQL using the root guide. With JDK 17+ and Maven 3.6.3+, export `MYSQL_APP_PASSWORD`, `HOSPITAL_PASSWORD`, and optionally the remaining variables above in your shell, then run:

```bash
mvn -f app/pom.xml spring-boot:run
```

Maven does not automatically load the root `.env` file. On Windows, set variables in PowerShell (for example `$env:HOSPITAL_USERNAME = 'staff'`) or your IDE's run configuration. Never put credentials in source files or Maven command-line properties.

## Testing

```bash
# Date conversion, DST, and report-boundary unit tests; no database needed.
mvn -B -ntp -f app/pom.xml verify

# Fresh disposable MySQL container, separate from the normal database volume.
docker compose --profile test up -d --wait database-test
python -m pip install -r database/v1/tests/requirements.txt
MYSQL_PORT=3308 MYSQL_ROOT_PASSWORD=hospital_test_only DB_SSL_MODE=DISABLED DB_ALLOW_PUBLIC_KEY_RETRIEVAL=true python app/tests/smoke_test.py
docker compose --profile test rm -sfv database-test
```

The HTTP suite creates the schema and synthetic fixtures, generates temporary passwords, launches the built JAR, and tests real login/CSRF behavior, every screen, registration/editing, escaped patient names, overlap rejection, UTC storage, rescheduling rollback, cancellation, completion rules, report boundaries, and database permissions. It shuts down the Java process afterward. It refuses to initialize over an existing `hospital_v1`: use a fresh test container for each run, and separate containers for database and app suites.

CI runs the same tests against MySQL 8.4, then uses Playwright with the runner's Chrome browser to check sign-in, registration, booking navigation, and desktop/mobile page overflow. It saves screenshots, Java test reports, and a server log. Python and Node are test tooling; the application is Java.

## Implementation references

- [Spring Boot requirements](https://docs.spring.io/spring-boot/4.1/system-requirements.html)
- [Spring Security CSRF protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [JDBC procedure transaction contract](../database/v1/README.md#examples-for-the-java-backend)
