---
name: clean-code-review
description: Review AllWage Java changes for clean code, SOLID design, package conventions, and test coverage. Use before completing implementation or when asked for a code review.
---

# Clean Code Review

Use the project `review` agent for a read-only review of the working-tree change. Review the diff and affected call paths, then report findings by severity with file and line references.

Check the following:

- Correctness, behavioral regressions, error handling, and missing focused tests.
- Simple responsibilities and dependency direction: controllers handle HTTP, services coordinate use cases, clients isolate external messaging, repositories isolate persistence access, and models remain independent.
- HTTP boundary mapping: request and response DTOs stay in `controller.<resource>`; controllers manually map request fields to domain models and domain results to response DTOs. Services, repositories, clients, and models must not depend on controller DTOs. Use `ClockController`'s manual `ClockRequest` to `ClockEvent` construction as the project reference; do not introduce automated or reflection-based mappers.
- SOLID concerns that are concrete in the changed code: mixed responsibilities, abstractions coupled to implementations, unsuitable dependencies, or interfaces that force irrelevant methods.
- Established names and package placement: `controller.<resource>`, `repository.<aggregate>`, `*Repository`, `*DocumentStoreRepository`, and matching test packages.
- Existing ArchUnit restrictions and Checkstyle violations.
- Ensure good usage of descriptive and correct java docs and comments on classes and methods.
- For cases where an audit trail for an object is required, make sure to re-use @Audited annotation.

Address the highest-severity findings before completion. If no findings remain, state that and note any residual test or runtime risk.
