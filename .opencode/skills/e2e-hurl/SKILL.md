---
name: e2e-hurl
description: Write or change AllWage Hurl E2E journey tests. Use when adding or changing controller endpoints, Hurl files, or end-to-end HTTP behavior.
---

# Hurl End-to-End Tests

Store Hurl source files in `e2e/journeys/` and follow `e2e/README.md` for execution. Hurl tests exercise a separately running Spring Boot application; they complement, not replace, Maven tests.

- Inspect the public controller route and HTTP DTOs before writing a journey. Do not test service, repository, or `DocumentStore` internals.
- Keep each `.hurl` file self-contained. Establish the required operational data through HTTP first: create a site, employee, primary geofence, team, employee assignment, and validation rules needed by the feature.
- Use fixed, descriptive fixture IDs and document the fresh-application requirement. Do not depend on another Hurl journey running first.
- Assert every HTTP status and the response fields that establish the behavior. Include error-path journeys when an endpoint has meaningful public rejection behavior.
- When a required setup API does not exist, report the missing endpoint as a blocker. Never seed the in-memory store or add test-only HTTP endpoints to bypass it.
- Update an existing journey when it already represents the user behavior; otherwise add one focused journey with a kebab-case filename.

Start the service in one PowerShell terminal:

```powershell
mvn spring-boot:run
```

Run the focused journey in another:

```powershell
hurl --test --variable base_url=http://localhost:8080 e2e/journeys/<journey>.hurl
```

Run all journeys serially when their shared application state could otherwise conflict:

```powershell
hurl --test --jobs 1 --variable base_url=http://localhost:8080 --glob "e2e/journeys/*.hurl"
```
