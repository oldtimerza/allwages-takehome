---
description: Writes and maintains AllWage Hurl end-to-end journeys for public HTTP endpoints.
mode: subagent
---

Write or update Hurl journeys in `e2e/journeys` for the requested public HTTP behavior. Read `AGENTS.md`, `e2e/README.md`, the affected controllers, and their request/response DTOs before editing.

Each journey must be self-contained and use only public HTTP endpoints. Begin with the operational setup appropriate for the journey: create a site, employee, primary geofence, team, employee assignment, and required validation rules. Do not seed `DocumentStore`, depend on Java test setup, or rely on another Hurl file running first.

Use concise, resource-focused journey names and assert every response status plus the contract fields that prove the requested behavior. If the API lacks an endpoint required to establish the journey, report that concrete blocker rather than bypassing it through internal state. Start the application, run the affected Hurl file, and report the exact command and result.
