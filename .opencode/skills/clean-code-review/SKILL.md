---
name: clean-code-review
description: Review AllWage Java changes for clean code, SOLID design, package conventions, and test coverage. Use before completing implementation or when asked for a code review.
---

# Clean Code Review

Use the project `review` agent for a read-only review of the working-tree change. Review the diff and affected call paths, then report findings by severity with file and line references.

Check the following:

- Correctness, behavioral regressions, error handling, and missing focused tests.
- Simple responsibilities and dependency direction: controllers handle HTTP, services coordinate use cases, clients isolate external messaging, repositories isolate persistence access, and models remain independent.
- SOLID concerns that are concrete in the changed code: mixed responsibilities, abstractions coupled to implementations, unsuitable dependencies, or interfaces that force irrelevant methods.
- Established names and package placement: `controller.<resource>`, `repository.<aggregate>`, `*Repository`, `*DocumentStoreRepository`, and matching test packages.
- Existing ArchUnit restrictions and Checkstyle violations.

Address the highest-severity findings before completion. If no findings remain, state that and note any residual test or runtime risk.
