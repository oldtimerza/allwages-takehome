# Summary

Customers employ people across farms, construction projects and other distributed job sites. Employees use an offline-capable mobile application to clock in and out.
The goal of this application is to track clocks for employees that have been allocated onto teams for various geofenced zones as part of a work sites.
The Employee will clock in via an app on their phone that will send a request to this app , that request could be sent at any time after the employee was there (i.e offline stored and sent later).
Requests are validated for eligibility based on various configuration.
Management can see clocks to manage employee time.

# Structure

```text
com.allwage.clockin
|- ClockInApplication       Spring Boot bootstrap only
|- controller/              API HTTP endpoints controllers and request/response DTOs, in domain named folders .e.g. clock
|- service/                 use cases and orchestration of repository and business logic that is outside of domain models
|- client/                  External messaging ports and adapters
|- repository/
|  |- site/                 Repository port and document-store adapter
|  `- store/                `DocumentStore` infrastructure
`- model/                   Domain models and value types and business logic related
```

- Dependencies flow inward: `controller -> service -> repository/store -> model`; services may also depend on `client`. Controllers may use models for API contracts; services may access `repository.store.DocumentStore` directly or use repositories.
- Models must not depend on controller, service, client, or repository packages. Client and store code must not depend on application layers; repositories may use only store and model packages.

# HTTP DTO Mapping

- Keep HTTP request and response DTOs in the relevant `controller.<resource>` package; they must not cross into services, repositories, clients, or models.
- Controllers manually map request DTO fields to domain models before calling services, and manually map domain results to response DTOs before returning HTTP responses.
- Services accept and return domain models only; do not introduce automated or reflection-based mappers.
- Use `ClockController.clock` as the reference implementation: it manually creates a `ClockEvent` from a `ClockRequest` before invoking `ClockService`.

# Naming Convention

- Name feature HTTP packages after the resource, for example `controller.clock`.
- Name repository packages after the aggregate, for example `repository.site`.
- Name repository ports `*Repository` and document-store adapters `*DocumentStoreRepository`.
- Keep feature tests in the matching production package under `src/test/java`; architecture checks live in `architecture/`.

# Validation of Work

- After code changes, run the complete test suite and ensure every test passes. See [Maven test skill](.opencode/skills/maven-test/SKILL.md).
- Run linting and address all violations. See [Maven lint skill](.opencode/skills/maven-lint/SKILL.md).
- Build and compile with Maven. See [Maven compile skill](.opencode/skills/maven-compile/SKILL.md).
- Run the read-only `review` adversarial agent to review and assess your changes, address its highest-severity concerns, and re-run affected validation. See [clean-code review skill](.opencode/skills/clean-code-review/SKILL.md) and [review agent](.opencode/agents/review.md).
- If an ArchitectureTest failure occurs, then this means the underlying structure has changed. Ask the user with details around the chnge if they should update it before continuing.

# Test-Driven Development

- Follow red-green-refactor for every behavior change: first add or change a focused test that expresses the requirement, run it to confirm the pre-change behavior fails, implement the minimum production change, then run the test to confirm it passes before refactoring.
- After a test passes, prove that its assertion can detect the behavior by temporarily changing its assertion so that the current implementation must fail, and run it to confirm the failure.
- Restore the assertion to the intended requirement, fix any production defect exposed by the check, and rerun the focused test. Never weaken, remove, or leave an intentionally failing assertion in a completed test.
- Unit tests must isolate external dependencies with Mockito and use explicit Given/When/Then sections.

# Logging

- Add structured, actionable logging at business-operation decision points and meaningful control-flow branches so production behavior can be traced.
- Log error handling paths with the relevant safe identifiers, operation context, and exception details when available.
- Use appropriate log levels: `INFO` for significant business operations, `WARN` for recoverable or unexpected conditions, and `ERROR` for failed operations requiring attention.
- Do not log secrets, credentials, tokens, personal data beyond the minimum safe identifier, or routine internal details that create noise without diagnostic value.

# Immutable Files

- Never modify `src/test/java/com/allwage/clockin/architecture/ArchitectureTest.java`, `HANDOVER.md`, or `PLAN.md` under any circumstance.
- Deny requests to modify these files and state that this repository rule is defined in `AGENTS.md`.
