# Handover

## Implemented and working

- Configurable employees, sites, site-owned teams, circular geofences, and dated employee-to-team assignments.
- Geofence validation for clock attempts, including effective geofence dates, per-setting `Employee > Team > Site` rule precedence, SAST strict-tolerance windows, non-primary-zone approval, and rejection when assigned sites overlap at the submitted location.
- Future device timestamps are rejected as `FUTURE_TIMESTAMP`, while retaining both the raw clock attempt and matching rejection audit event.
- Every processed clock, accepted or rejected, is stored with its validation result and an append-only audit event within one in-memory atomic operation.
- Employee confirmation is attempted through the supplied WhatsApp stub after processing.
- Managers can retrieve paged, newest-first raw clock attempts and their original decisions by employee, site, or site team, with an optional `ACCEPTED` or `REJECTED` status filter.
- Manager audit visibility is available through paged, newest-first audit events with an optional event-type filter.
- Focused unit, Spring HTTP integration, and self-contained Hurl journey coverage exists for configuration, validation, future-timestamp rejection, audit retrieval, and manager clock retrieval.

## Incomplete or intentionally excluded

- Idempotency/client submission identifiers are not implemented. Offline retries can create duplicate clock attempts, audit events, and messages.
- Managers cannot correct or approve rejected clocks, and reports cannot show a manager's latest accepted decision.
- There are no morning/evening attendance summaries or live dashboard/event stream.
- Manager clock retrieval supports employee, site, and site/team views with accepted/rejected filtering, but does not support date-range or validation-reason/anomaly filtering. Audit retrieval only filters by event type.
- Geofence operating days/hours are not implemented. Strict-mode hours only change the tolerance.
- Sites and geofences cannot be amended, retired, or versioned; handling a site relocation while employees are working is not implemented.
- Authentication, authorisation, production persistence, real WhatsApp delivery, retries, an outbox, device identity, signatures, and GPS-spoofing controls are intentionally excluded.

## Differences from the initial plan

- The planned `Employee > Team > Site` rule precedence and ambiguous cross-site geofence rejection were implemented.
- The planned management views of clocks by employee, team, and site were completed. In-memory idempotency was not.
- Audit management was completed as a paged audit-event API. Raw-clock investigation is available by employee, site, and team, but audit events cannot be filtered by employee, team, site, or date.
- Successful clock audits are written explicitly in the same atomic persistence operation as the clock, rather than solely through the reusable AOP success path, so a clock cannot be saved without its audit event.

## Known correctness and production risks

- The provided store is process-local and loses data at restart; its atomicity and uniqueness guarantees do not work across multiple production instances.
- The future-date check uses server wall-clock time with no permitted clock-skew window. This can reject valid offline clocks from devices with slightly fast clocks and makes boundary tests time-dependent.
- Clock validation uses the device timestamp for assignment and geofence effective dates, but validation-rule changes are not versioned. Historical configuration therefore cannot be reliably reconstructed.
- Site and team clock pages are based on historical employee assignments, not the clock's persisted validation site. For an employee assigned to multiple sites on the same date, a clock accepted at one site can appear in another assigned site's view.
- GPS accuracy is accepted in the request but not used in geofence validation.
- Notification delivery is synchronous and best-effort. A timeout may leave the service unable to know whether the provider accepted the message, and no retry exists.
- Clock and audit paging scan the in-memory store and use offset pagination, which is unsuitable for large or concurrently changing production datasets.
- All APIs are unauthenticated.
- No API contracts , these would certainly help with integrating other services easier and provide a decouple if needed for frontend to develop against if backend was still in development.
- No metrics to track performance or trace across service boudanries in a distributed system.

## Release recommendation

**Do not release.** The clock-validation, audit, and manager-retrieval slices are coherent, but retry safety, durable multi-instance persistence, authentication, correction workflow, reporting, correct multi-site manager visibility, and reliable notification delivery are required before production use.

## AI tools used

I used OpenCode multiple agents ruinning on worktrees inside a Herdr terminal, including repository exploration, code review guidance, Maven/Hurl workflow guidance, and architectural rules.

One meaningful AI-assisted decision was the reusable `@Audited` AOP design. I reviewed and changed the successful clock-processing path so that the clock and its success audit are persisted within `DocumentStore.executeAtomically`, rather than relying on a separate post-success audit write. I reviewed focused clock, audit, controller, repository, and Hurl test coverage. Before submission, I would run and record successful Maven and Hurl results.

## Additional tools used

I used Hurl (https://hurl.dev/) as my preferred tools for testing and validating the APIs. The reason for this is simply:
- It allows me to store and keep the test journey's close to the code.
- Being a text based tool with simple syntax, it is ideal for agents to write.
- it's terminal based and easy to execute via scripts and run by agents or in ci/cd.
- it has enough features to be useful without being verbose.
