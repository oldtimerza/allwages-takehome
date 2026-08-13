---
name: integration-test
description: Write or change AllWage HTTP integration tests using ClockControllerTest as the project example. Use when testing controller endpoints across the running Spring application and in-memory store.
---

# Integration Test

Use `src/test/java/com/allwage/clockin/controller/clock/ClockControllerTest.java` as the established example for HTTP integration tests.

- Place feature integration tests in the matching production package under `src/test/java`.
- Start the full application with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`.
- Inject `TestRestTemplate` to make real HTTP requests to the running application.
- Inject `repository.store.DocumentStore` when the test must arrange or verify persisted documents.
- Clear every store collection used by a test in `@BeforeEach`, because the document store is shared, in-memory state.
- Exercise the endpoint through HTTP, assert the status and response body, then verify important persisted state through `DocumentStore` where appropriate.
- Build JSON requests with `application/json` headers and use the endpoint's public request and response contracts.
- Keep tests focused on observable behavior; do not mock application components in these integration tests.

Run a focused test while iterating:

```powershell
mvn -Dtest=ClockControllerTest test
```

Run the full test suite before completing the change:

```powershell
mvn test
```
