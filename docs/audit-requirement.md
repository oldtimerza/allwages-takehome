# Clock Audit Requirement

## Purpose

Maintain a durable, immutable, append-only record of every clock submission attempt. The audit trail supports operational investigation, payroll disputes, and compliance without duplicating sensitive location data.

This applies to all submissions received by `POST /api/clocks`, including accepted clocks, business rejections, malformed requests, validation failures, and processing failures.

## Audit Event Model

Persist audit events separately from clock events in a `clock-audits` collection. An audit event contains:

- `id`: server-generated audit event ID.
- `occurredAt`: server-side time at which the event was recorded.
- `correlationId`: request identifier used to connect API logs, clock records, and audit records.
- `clockEventId`: identifier of the related clock event; absent when no valid clock event can be created.
- `employeeId`: employee identifier when it can be safely obtained from the submission.
- `action`: `ACCEPTED`, `REJECTED`, or `FAILED`.
- `reasonCode`: stable machine-readable reason, for example `REQUEST_VALIDATION_FAILED`, `GEOFENCE_REJECTED`, or `PERSISTENCE_FAILED`.
- `source`: submission origin, initially `MOBILE_API`.
- `httpStatus`: HTTP result returned to the caller.
- `safeRequestMetadata`: non-sensitive request context, such as route, HTTP method, and an app or device identifier when available.

Audit events must not include raw request bodies, coordinates, phone numbers, credentials, tokens, exception messages, or notification content. The linked `ClockEvent` remains the authoritative location record.

## Processing and Persistence

The audit trail is a business record, not an application log. Structured logs remain useful for operations but cannot replace durable audit events.

For every processed clock, persist the clock event with its original validation result and the matching audit event atomically:

1. `clocks/{clockEventId}` stores a `ValidatedClockEvent`, containing the original `ClockEvent` and its validation result.
2. `clock-audits/{auditEventId}` stores the linked audit event.

If either write fails, the request fails. The service must not return a success response or retain an accepted clock without the associated audit record. The current `DocumentStore` has independent writes only, so a generic atomic multi-document write is required at the store boundary. A production database implementation should use a transaction with the same contract.

Business-rule rejections retain their `ClockEvent` and its `REJECTED` audit event so managers can review the original attempt. Requests that fail controller-level validation cannot produce a valid event and retain only their audit event. If an audit event cannot be written, return a server failure rather than the original rejection, because every submission attempt must be durably auditable.

### Implemented Baseline

The first implementation uses a synchronous audit write so an accepted clock and its audit event are committed together. `DocumentStore.executeAtomically` provides rollback-capable, in-memory atomicity for the current assessment store. Audit persistence uses insert-if-absent semantics, so an existing audit event ID cannot be overwritten.

Service use cases opt in with a one-line annotation:

```java
@Audited(mapper = ClockProcessingAuditMapper.class)
```

The shared aspect invokes the mapper after a successful service operation and writes the resulting audit event within the same atomic operation. Mappers are Spring components that convert operation arguments and results into typed audit payloads. The annotation references a mapper class only in application code; persisted documents use stable `type` and `schemaVersion` fields, never Java class names.

`ClockAuditExceptionHandler` creates `CLOCK_REJECTED` events for malformed and Bean Validation-rejected requests that do not reach the annotated service method. `CorrelationIdFilter` creates a server-generated correlation ID for each HTTP request and returns it in `X-Correlation-Id`.

The current implementation intentionally does not use a non-blocking outbox. If asynchronous delivery becomes necessary, replace the synchronous audit writer with a transactional outbox implementation while retaining the `@Audited` and mapper interfaces.

## Integration Points

- `ClockService.processClock`: the primary audit decision point for accepted clocks and future domain rules, including eligibility, allocation, geofence, timestamp plausibility, and notification outcomes.
- MVC request boundary: establish or accept a validated correlation ID, capture route and request timing, and audit malformed requests that cannot reach the service.
- Controller exception handler: convert validation and processing errors into stable API error codes and corresponding audit reason codes.
- Persistence boundary: provide atomic clock-plus-audit persistence for accepted clocks.
- Messaging boundary: when messaging is introduced, capture notification attempt and result as an additional audit action without recording phone numbers or message bodies.

## Submission Lifecycle

| Condition | Clock event | Audit action | Example reason code |
| --- | --- | --- | --- |
| Valid and accepted | Persist atomically with audit event | `ACCEPTED` | `CLOCK_ACCEPTED` |
| Valid but rejected by a business rule | Persist with original rejected result | `REJECTED` | `GEOFENCE_REJECTED` |
| Missing or invalid request fields | Do not persist | `REJECTED` | `REQUEST_VALIDATION_FAILED` |
| Malformed JSON or unreadable request | Do not persist | `REJECTED` | `REQUEST_MALFORMED` |
| Processing or persistence failure | Do not persist accepted clock unless atomic operation completes | `FAILED` where persistence remains available | `PERSISTENCE_FAILED` |

## Logging and Access

Use structured application logs at meaningful decision points with `correlationId`, audit event ID, clock event ID, action, and reason code. Do not log raw locations, request bodies, tokens, phone numbers, or notification content.

Audit records should be read only through a management-authorized API when authentication and authorization are added. They must not have mutation or deletion endpoints. Retention should be longer than normal clock records and defined with payroll and compliance stakeholders.

## Idempotency

Offline mobile submissions may be retried. Before relying on audit data for payroll dispute resolution, add a client-generated submission ID. Store it on a separate idempotency record so repeated delivery can be classified as a retry rather than a new clock attempt.

## Acceptance Tests

1. An accepted submission creates one clock event and one linked `ACCEPTED` audit event.
2. A business rejection creates one clock event with its original rejected result and one `REJECTED` audit event with a stable reason code.
3. Bean-validation failures and malformed JSON create audit events even though `ClockService` is not called.
4. An audit persistence failure prevents a successful response and prevents an accepted clock from being retained.
5. Audit events omit coordinates, raw request payloads, phone numbers, credentials, tokens, exception messages, and notification content.
6. A correlation ID links request logs, clock events, and audit events.
7. No application API can mutate or delete an audit event.
