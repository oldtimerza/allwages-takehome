# Engineering Lead Technical Assessment

## Time limit

**Strict working-time limit: 2.5 hours**

The brief intentionally contains more than can be completed in the available time. We are interested in how you interpret the problem, make assumptions, choose what matters and verify what you build.

Stop after 2.5 hours. An incomplete but coherent and well-tested submission is preferable to working beyond the limit.

## AI-assisted development

AI-assisted development is part of how we work at AllWage. We encourage you to use the AI coding and research tools you would normally use, including agentic coding tools.

We are interested in how you direct these tools, provide context, evaluate their output and verify the result. You remain responsible for every part of the submission, regardless of who or what produced the first version. In the follow-up discussion, we will ask you to explain your implementation, evaluate alternative recommendations and discuss what you would be willing to release.

## Technology

- Java 21 or later
- Spring Boot 3.2.5 or later
- The provided in-memory, document-oriented store

AllWage uses a NoSQL document database in production. For the assessment, use the provided store and model the data as documents around the access patterns you choose. Do not replace it with a relational database.

## Starting point

The starter project contains:

- A working `POST /api/clocks` endpoint that saves a clock attempt.
- Basic read endpoints for clock attempts.
- An in-memory document store.
- A stubbed WhatsApp client.
- One example integration test.

The starter is deliberately small. You may change its APIs and models where the product brief requires it.

## Product brief: geofenced clocking

AllWage customers employ people across farms, construction projects and other distributed job sites. Employees use an offline-capable mobile application to clock in and out.

Customers want the backend to:

1. Determine whether a clock occurred within a valid geofence.
2. Send the employee a WhatsApp confirmation indicating whether the clock was accepted or requires attention.
3. Give managers visibility of valid clocks, invalid clocks and anomalies.
4. Maintain an immutable audit record of every clock attempt and its validation result.
5. Support a live dashboard that can receive clock events as they are processed.
6. Provide managers with morning and evening attendance summaries.

### Sites, zones and assignments

- A site may have more than one valid circular geofence, such as a main entrance and an equipment yard.
- A site may move over the lifetime of a project.
- An employee may be assigned to more than one site.
- Each site assignment places the employee in exactly one team at that site.
- Geofences are circles defined by a centre coordinate and radius. Polygon geofences are out of scope.

The mobile application currently sends the employee, device timestamp, coordinates, GPS accuracy and clock type. You may change the request contract if your design requires additional information.

### Temporal behaviour

- Geofences may have effective start and end dates.
- A zone may only operate during certain hours or days, such as weekdays from 06:00 to 18:00.
- Product expects validation to use the geofence configuration that is active when the request is processed, so that the employee receives the current answer.
- Operations expects an offline clock to be judged according to the conditions that applied when the employee originally clocked, even if it is synchronised hours later.
- Assume the business currently operates in SAST (UTC+2). Multi-timezone support is out of scope.

### Validation rules

Validation rules may be configured at three levels:

`Site -> Team -> Employee`

The available settings include:

| Setting | Description | Default |
|---|---|---|
| Geofence tolerance | Additional distance allowed beyond the configured radius | 20 metres |
| Strict-mode hours | Time windows during which a tighter tolerance applies | None |
| Approval required | Whether a clock outside the primary zone requires manager approval | No |

Example:

- Site Alpha allows a 30-metre tolerance.
- The Contractors team uses a 10-metre tolerance and requires approval outside the primary zone.
- One contractor has a 50-metre tolerance because the designated parking area is further away.

The brief does not define how partial settings at different levels should be merged. Choose and document a consistent interpretation.

### Corrections and auditability

- Raw clock attempts and their original validation results must remain available for audit.
- Managers must be able to correct or approve a clock that was incorrectly rejected.
- Changing a site or geofence should take effect without disrupting employees who are currently working.
- Reports should show the manager's latest accepted decision.

### Connectivity and side effects

- The mobile application queues requests while offline and retries when it does not receive a response.
- A retry may be handled by a different backend instance.
- Employees expect confirmation immediately after clocking, including at sites with unreliable connectivity.
- The WhatsApp provider may be slow, reject a request or time out after accepting it.
- A single employee may legitimately submit more than one clock of the same type close together.

### Runtime environment

- Production runs multiple backend instances concurrently.
- The assessment uses a single-process in-memory store and loses data on restart.
- You do not need to introduce production infrastructure, but your design should make its production assumptions and limitations clear.

## Your task

Extend the starter project with the most coherent and valuable slice of this product that you can deliver within 2.5 hours.

You decide what to implement, what to leave out and how to resolve missing or conflicting requirements. We expect working application code and automated tests around at least one complete clock-processing path. We do not expect every item in the product brief to be implemented.

Do not spend the assessment building external infrastructure or integrating a real WhatsApp provider. Use the supplied abstractions and document where production behaviour would differ.

## Required deliverables

### 1. `PLAN.md`

Complete this before implementation and keep it to approximately one page. Spend no more than 20 minutes on it.

Include:

- Your understanding of the most important problems and risks.
- The assumptions you are making where the brief is missing or inconsistent.
- What you intend to complete in the time available.
- What you are intentionally not implementing.
- How you plan to verify the result.

Leave the initial plan intact. If your thinking changes during implementation, describe that in the handover rather than rewriting the plan to match the final result.

### 2. Implementation and tests

Submit the source code required to build and run your solution, together with focused automated tests. We value tests that expose your assumptions and important failure cases more than a coverage percentage.

### 3. `HANDOVER.md`

Keep this to approximately one page and include:

- What is implemented and working.
- What is incomplete or intentionally excluded.
- Where your implementation differs from your initial plan and why.
- Known correctness or production risks.
- Whether you would release this as-is, release it behind a limited rollout, or not release it.
- Which AI tools you used, one meaningful AI-assisted decision or output you reviewed, and how you verified, changed or rejected it.

### 4. Setup instructions

Update the project README only if the supplied build, run or test instructions no longer apply.

## What we evaluate

- Prioritisation within the time limit.
- Reasoning through ambiguity and conflicting needs.
- Backend and data-model design.
- Correctness of the implemented flow.
- Quality and relevance of tests.
- Production judgment and explicit trade-offs.
- Effective direction and verification of AI-assisted work.
- Ownership of the final submission.

The follow-up interview is two hours. We will spend the first hour discussing your work history and experience, and the second hour walking through your implementation and related production scenarios. It is not a live coding continuation.

## Submission

Return the completed project as a GitHub repository link. If the repository is private, ensure that the AllWage reviewers have access. Do not commit generated build directories such as `target`.
