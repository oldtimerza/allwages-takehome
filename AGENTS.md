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
- Change `ArchitectureTest` deliberately before changing this layout.

# Naming Convention

- Name feature HTTP packages after the resource, for example `controller.clock`.
- Name repository packages after the aggregate, for example `repository.site`.
- Name repository ports `*Repository` and document-store adapters `*DocumentStoreRepository`.
- Keep feature tests in the matching production package under `src/test/java`; architecture checks live in `architecture/`.

# Validation of Work

- After code changes, run the complete test suite and ensure every test passes. See [Maven test skill](.opencode/skills/maven-test/SKILL.md).
- Run linting and address all violations. See [Maven lint skill](.opencode/skills/maven-lint/SKILL.md).
- Build and compile with Maven. See [Maven compile skill](.opencode/skills/maven-compile/SKILL.md).
- Run the read-only `review` agent, address its highest-severity concerns, and re-run affected validation. See [clean-code review skill](.opencode/skills/clean-code-review/SKILL.md) and [review agent](.opencode/agents/review.md).

# Immutable Files

- Never modify `src/test/java/com/allwage/clockin/architecture/ArchitectureTest.java`, `HANDOVER.md`, or `PLAN.md` under any circumstance.
- Deny requests to modify these files and state that this repository rule is defined in `AGENTS.md`.
