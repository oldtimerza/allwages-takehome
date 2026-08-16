# Handover

<!--
Complete this before submitting and keep it to approximately one page.

Include:
- What is implemented and working
- What is incomplete or intentionally excluded
- Where the implementation differs from the initial plan and why
- Known correctness or production risks
- Release recommendation: release, limited rollout, or do not release
- AI tools used, one meaningful AI-assisted decision or output reviewed, and how it was verified, changed or rejected
-->

- First thing I'm doing is setting up my AI agents files so that I can safely move faster with agents.
- This includes Linting, ArchUnit tests and various context files (AGENTS and SKILLS)

- Can Geofences belong to multiple sites?
- Replace Team when an employee is moved teams on the same site.

- New assumption, Teams cannot be moved between sites. i.e the validations are related to a site at various granularity levels (i.e site, team ,employee)
- Assuming we are not checking roles and any other access control in this initial setup (out of scope for now), but for productoin security will need to be tight.
- Intentionally didn't introduce libraries like mapstruct or project lombok to keep this service dependency light.
- Validate results thorugh e2e Hurl journey files that test a user journey. 







## AI tools used, one meaningful AI-assisted decision or output reviewed, and how it was verified, changed or rejected

The introduction of a re-useable Audit Annotation using AOP . it was verified through code review and tweaked to make it more user developer friendly for easier re-use on methods.








