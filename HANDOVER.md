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