---
name: maven-compile
description: Compile the AllWage Spring Boot application with Maven. Use when building, compiling, or checking whether production code compiles.
---

# Maven Compile

Run from the repository root:

```powershell
mvn clean compile
```

This project requires Java 21 or later. Checkstyle runs in Maven's `validate` phase, so compilation also verifies linting. Do not treat a successful command as a test run; run the test skill separately.

If `mvn` is unavailable, report that Maven is not installed or not on `PATH`; do not claim compilation passed.
