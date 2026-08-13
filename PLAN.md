# Initial Plan

<!--
Complete this before implementing and leave the initial plan intact.
Keep it to approximately one page and spend no more than 20 minutes on it.

Include:
- The most important problems and risks you see
- Assumptions made where the product brief is missing or inconsistent
- What you intend to complete within 2.5 hours
- What you are intentionally not implementing
- How you will verify the result
-->

### Priority
Order of priority for this to meet the customers requirements in the given time frame:

#### Needs (can't go live without it):

- Correct validation of employees assigned to a team on that site, their time of clock in and the correct location of the site they clocked into.
- Immutable Audit trail for clock attempts and validation result.
- Correct configuratoin of validation settings.
- Idemptotency checks on incoming clock requests to prevent duplication (in memory for now).
- Site configuration: circular geofences associated with a site.
- Assignment of an Employee to a Team for a Site.
- To send employee indication of accepted or requires attention.
- Site location update and correct handling employees still at work.

#### Nice to haves (can go live without but include if time permits):

- Hash signature on clock entry to ensure no tampering occured.
- Tamper seal on audit entries to ensure database tampering didn't occur.
- Morning and evening attendance summaries (this can be done manual in a worst case scenario).
- Correction of incorrectly rejected clocks attempts.
- Whatsapp confirmation retry in case of failure.

#### Can come later (can rethink and plan later):

- websockets for realtime dashboard.
- Audit trail for site management (i.e site location moves)
- "Teleportation" check , i.e spoofing of location detection.
- Automatic public holiday detection.
- Device registration and detection to prevent other device usage (later or another employee clock you in)
- Metrics and instrumentation beyond simple logging.

### Assumptions

- Sites have a simple start and end, not a more complex time schedule.
- Employee vacation days do not need to be tracked or adhered to in this instance.
- Accuracy of geozones must just be to the meter, sub-meter accuracy isn't necessary.
- The timestamp on the request ( device timestamp) is the time that is used for validation of clock in within elgibile hours.
- Assuming the following merge rules for site validation: Employee > Team > Site i.e specific beats general.
- Every clock attempt must be recorded, but only validated correct ones count.
- The notification of the clock requiring attention could potentially come hours after the initial clock occured ( due to offline nature.), o a simple message to contact manager will be done.
- Assume site location updates do not require additional approval.
- No Identity provider is used for this , so no difference in requests by manager vs clock from employee at the moment. (this will need to be considered in future)
- 

### Intend to complete

- Setup of Sites, Teams, Employees and Validation configuration.
- Recording of original clocks data with immutable audit trail.
- Clock Validation with audit of results.
- Ability for managers to retrieve clock attempts information per employee/ per team /per site.
- Ability for managers to retrieve audit records for clock attempts.
- Send employee a message whether clock was accepted or requires attention.
- Automation and Unit tests (TDD)
- De-duplication of clocks.

### Stretch Goal:

- Reports.
- Manager edit rejected clocks ability.
- Site location update and historic capture of working employees on this site.
- Correct handling of employees still at work on a site that has moved.

### Intentionally not implementing:

- Websockets for realtime dashboard. Polling can do the job initially.
- Reports , these can become quiet hairy to get right in a performant manner.
- Updating of rejected clocks (Roles and permissions and other things related to this need to be thought of)

### Verification of the result:

- Hurl files for E2E verification of system correctness.
- Unit tests as standard as part of TDD.
- Integration tests.