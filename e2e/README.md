# End-to-End Tests

Hurl journeys exercise the running service through its public HTTP API. They complement the Java unit and integration tests; they do not run as part of Maven.

## Prerequisites

- Java 21 and Maven
- [Hurl](https://hurl.dev/) 7 or later

## Run A Journey

Start a fresh application in one PowerShell terminal from the repository root:

```powershell
mvn spring-boot:run
```

After the application reports that it is listening on port 8080, run a journey from another terminal:

```powershell
hurl --test --variable base_url=http://localhost:8080 e2e/journeys/site-configuration.hurl
```

The application uses an in-memory document store. Restart it before rerunning a journey so its fixed fixture identifiers do not conflict with data from a previous run.

To run all journeys serially and create an HTML report:

```powershell
hurl --test --jobs 1 --variable base_url=http://localhost:8080 --report-html e2e/reports --glob "e2e/journeys/*.hurl"
```

`e2e/reports/` contains generated output and is not committed.

## Layout

```text
e2e/
|- README.md                   This guide
|- journeys/                   Versioned Hurl journey files
`- reports/                    Generated Hurl reports, ignored by Git
```

Each journey is self-contained and creates its own data using public HTTP endpoints. Begin with the shared operational setup: create a site, employee, primary geofence, team, and assignment; then set any validation rules required by the behavior under test. Do not seed the in-memory `DocumentStore` or rely on a prior Hurl file having run.
