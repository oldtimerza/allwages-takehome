---
name: maven-test
description: Run AllWage Maven tests, including focused controller and architecture checks. Use when validating tests or diagnosing a test failure.
---

# Maven Test

Run the full suite from the repository root:

```powershell
mvn test
```

Run focused tests when iterating:

```powershell
mvn -Dtest=ClockControllerTest test
mvn -Dtest=ArchitectureTest test
```

`ClockControllerTest` starts the full application with a random port and uses the shared in-memory store. Tests that create additional collections must clear their own data.

If `mvn` is unavailable, report that Maven is not installed or not on `PATH`; do not claim tests passed.
