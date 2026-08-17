# Create the site, employee, primary geofence, team, assignment, and validation rules.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "name": "E2E Manager Clocks Site",
  "validationRules": null
}
HTTP 201
[Captures]
site_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Manager Clocks Site"

POST {{base_url}}/api/employees
Content-Type: application/json
{
  "name": "E2E Manager Clocks Employee",
  "phoneNumber": "+27115550130"
}
HTTP 201
[Captures]
employee_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Manager Clocks Employee"
jsonpath "$.phoneNumber" == "+27115550130"

POST {{base_url}}/api/sites/{{site_id}}/geofences
Content-Type: application/json
{
  "latitude": -26.2041,
  "longitude": 28.0473,
  "radiusMeters": 100,
  "primary": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
HTTP 201
[Captures]
geofence_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.primary" == true
jsonpath "$.effectiveFrom" == "2026-01-01"
jsonpath "$.effectiveTo" == null

POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Historical Team",
  "validationRules": null
}
HTTP 201
[Captures]
historical_team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Historical Team"

POST {{base_url}}/api/sites/{{site_id}}/teams/{{historical_team_id}}/assignments
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "{{employee_id}}"
jsonpath "$.teamId" == "{{historical_team_id}}"
jsonpath "$.effectiveFrom" == "2026-01-01"

PUT {{base_url}}/api/sites/{{site_id}}/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 0,
  "approvalRequired": false,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 0
jsonpath "$.approvalRequired" == false
jsonpath "$.strictModeHours" == null

# On 2026-08-17 the employee moves teams. The 2026-08-16 device timestamps
# below must remain associated with the historical team.
POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Current Team",
  "validationRules": null
}
HTTP 201
[Captures]
current_team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Current Team"

POST {{base_url}}/api/sites/{{site_id}}/teams/{{current_team_id}}/assignments
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "effectiveFrom": "2026-08-17",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "{{employee_id}}"
jsonpath "$.teamId" == "{{current_team_id}}"
jsonpath "$.effectiveFrom" == "2026-08-17"

# Submit out of device-timestamp order so retrieval must sort newest device timestamp first.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "timestamp": "2026-08-16T11:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "OUT"
}
HTTP 200
[Asserts]
jsonpath "$.clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.clockEvent.type" == "OUT"
jsonpath "$.validationResult.decision" == "ACCEPTED"
jsonpath "$.validationResult.reason" == "ACCEPTED"
jsonpath "$.validationResult.siteId" == "{{site_id}}"
jsonpath "$.validationResult.geofenceId" == "{{geofence_id}}"

POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "timestamp": "2026-08-16T09:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.clockEvent.timestamp" == "2026-08-16T07:00:00Z"
jsonpath "$.validationResult.decision" == "ACCEPTED"
jsonpath "$.validationResult.siteId" == "{{site_id}}"

POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "timestamp": "2026-08-16T10:00:00+02:00",
  "latitude": -26.2141,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.validationResult.decision" == "REJECTED"
jsonpath "$.validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.validationResult.siteId" == null
jsonpath "$.validationResult.geofenceId" == null

# Employee pages include accepted and rejected attempts, newest device timestamp first.
GET {{base_url}}/api/employees/{{employee_id}}/clocks?page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 2
jsonpath "$.entries[0].clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "ACCEPTED"
jsonpath "$.entries[1].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[1].validationResult.decision" == "REJECTED"
jsonpath "$.entries[1].validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# Site pages resolve every attempt through its historical SAST-date assignment.
GET {{base_url}}/api/sites/{{site_id}}/clocks?page=1&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T07:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "ACCEPTED"
jsonpath "$.page" == 1
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# The historical team includes the rejected attempt; the current team does not.
GET {{base_url}}/api/sites/{{site_id}}/teams/{{historical_team_id}}/clocks?page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 2
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.entries[1].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[1].validationResult.decision" == "REJECTED"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# Status filtering preserves the employee page metadata and returns only rejected attempts.
GET {{base_url}}/api/employees/{{employee_id}}/clocks?status=REJECTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "REJECTED"
jsonpath "$.entries[0].validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 1
jsonpath "$.totalPages" == 1

# Status filtering preserves the site page metadata and returns only accepted attempts.
GET {{base_url}}/api/sites/{{site_id}}/clocks?status=ACCEPTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 2
jsonpath "$.entries[0].clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "ACCEPTED"
jsonpath "$.entries[0].validationResult.siteId" == "{{site_id}}"
jsonpath "$.entries[1].clockEvent.timestamp" == "2026-08-16T07:00:00Z"
jsonpath "$.entries[1].validationResult.decision" == "ACCEPTED"
jsonpath "$.entries[1].validationResult.siteId" == "{{site_id}}"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 2
jsonpath "$.totalPages" == 1

# Status filtering preserves the team page metadata and returns only rejected attempts.
GET {{base_url}}/api/sites/{{site_id}}/teams/{{historical_team_id}}/clocks?status=REJECTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "{{employee_id}}"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "REJECTED"
jsonpath "$.entries[0].validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 1
jsonpath "$.totalPages" == 1

GET {{base_url}}/api/sites/{{site_id}}/teams/{{current_team_id}}/clocks?page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 0
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 0
jsonpath "$.totalPages" == 0

# A page beyond the result set is valid and returns no entries.
GET {{base_url}}/api/employees/{{employee_id}}/clocks?page=3&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 0
jsonpath "$.page" == 3
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# Invalid page sizes are rejected before lookup.
GET {{base_url}}/api/employees/{{employee_id}}/clocks?size=0
HTTP 400

# Unknown employee, site, and team resources return 404.
GET {{base_url}}/api/employees/e2e-manager-clocks-unknown-employee/clocks?page=0&size=2
HTTP 404

GET {{base_url}}/api/sites/e2e-manager-clocks-unknown-site/clocks?page=0&size=2
HTTP 404

GET {{base_url}}/api/sites/{{site_id}}/teams/e2e-manager-clocks-unknown-team/clocks?page=0&size=2
HTTP 404
