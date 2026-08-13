---
name: maven-lint
description: Run Checkstyle linting for the AllWage Java source and tests. Use after Java changes or when resolving style violations.
---

# Maven Lint

Run from the repository root:

```powershell
mvn checkstyle:check
```

Checkstyle is also bound to Maven's `validate` phase, so `mvn clean compile` and `mvn test` run it automatically. The rules are in `checkstyle.xml` and apply to both production and test Java sources.

If `mvn` is unavailable, report that Maven is not installed or not on `PATH`; do not claim linting passed.
