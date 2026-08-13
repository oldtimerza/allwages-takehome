# AllWage Clock-In Assessment

## Build and test

- Requires Java 21+ and Maven 3.8+. Build with `mvn clean compile`, run with `mvn spring-boot:run`, and run the suite with `mvn test`.
- Run the current controller integration test alone with `mvn -Dtest=ClockControllerTest test`.
- No lint, formatter, code-generation, or CI configuration is currently present; Maven compilation and tests are the available automated checks.

## Application shape

- This is one Spring Boot application rooted at `com.allwage.clockin.ClockInApplication`; HTTP endpoints begin in `controller/`, with processing in `service/`.
- `DocumentStore` is the required persistence mechanism for this assessment. It is a process-local `ConcurrentHashMap` document store, so data is lost on restart; do not introduce a relational database.
- `POST /api/clocks` constructs the raw `ClockEvent` and delegates processing to `ClockService`; `ClockRequest` uses `ZonedDateTime` and the supplied contract assumes SAST (UTC+2).
- Use the `WhatsAppClient` abstraction for confirmations. The only supplied implementation, `WhatsAppClientStub`, logs messages and returns success; do not integrate a real provider.

## Tests and assessment deliverables

- `ClockControllerTest` is a full `@SpringBootTest` using a random port and the shared in-memory store. It clears the `clocks` collection before each test; tests that add collections must isolate their own data.
- Keep `PLAN.md` as the original pre-implementation plan. Record changed decisions, limitations, release recommendation, and AI-use verification in `HANDOVER.md` instead.
- Favor a coherent, tested clock-processing slice over broad infrastructure. Explicitly document production limitations of in-memory state, especially restart and multi-instance behavior.
- Do not commit `target/`; update `README.md` only if its build, run, or test instructions stop applying.
