# Requires a fresh application because this journey uses fixed fixture identifiers.
# Create the site, employee, primary geofence, team, assignment, and validation rules.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "id": "e2e-manager-clocks-site",
  "name": "E2E Manager Clocks Site",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-manager-clocks-site"
jsonpath "$.name" == "E2E Manager Clocks Site"

POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-manager-clocks-employee",
  "name": "E2E Manager Clocks Employee",
  "phoneNumber": "+27115550130"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-manager-clocks-employee"
jsonpath "$.name" == "E2E Manager Clocks Employee"
jsonpath "$.phoneNumber" == "+27115550130"

POST {{base_url}}/api/sites/e2e-manager-clocks-site/geofences
Content-Type: application/json
{
  "id": "e2e-manager-clocks-primary-zone",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "radiusMeters": 100,
  "primary": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-manager-clocks-primary-zone"
jsonpath "$.primary" == true
jsonpath "$.effectiveFrom" == "2026-01-01"
jsonpath "$.effectiveTo" == null

POST {{base_url}}/api/sites/e2e-manager-clocks-site/teams
Content-Type: application/json
{
  "id": "e2e-manager-clocks-historical-team",
  "name": "E2E Historical Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-manager-clocks-historical-team"
jsonpath "$.name" == "E2E Historical Team"

POST {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-historical-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-manager-clocks-employee",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.teamId" == "e2e-manager-clocks-historical-team"
jsonpath "$.effectiveFrom" == "2026-01-01"

PUT {{base_url}}/api/sites/e2e-manager-clocks-site/validation-rules
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
POST {{base_url}}/api/sites/e2e-manager-clocks-site/teams
Content-Type: application/json
{
  "id": "e2e-manager-clocks-current-team",
  "name": "E2E Current Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-manager-clocks-current-team"
jsonpath "$.name" == "E2E Current Team"

POST {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-current-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-manager-clocks-employee",
  "effectiveFrom": "2026-08-17",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.teamId" == "e2e-manager-clocks-current-team"
jsonpath "$.effectiveFrom" == "2026-08-17"

# Submit out of device-timestamp order so retrieval must sort newest device timestamp first.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-manager-clocks-employee",
  "timestamp": "2026-08-16T11:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "OUT"
}
HTTP 200
[Asserts]
jsonpath "$.clockEvent.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.clockEvent.type" == "OUT"
jsonpath "$.validationResult.decision" == "ACCEPTED"
jsonpath "$.validationResult.reason" == "ACCEPTED"
jsonpath "$.validationResult.siteId" == "e2e-manager-clocks-site"
jsonpath "$.validationResult.geofenceId" == "e2e-manager-clocks-primary-zone"

POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-manager-clocks-employee",
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
jsonpath "$.validationResult.siteId" == "e2e-manager-clocks-site"

POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-manager-clocks-employee",
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
GET {{base_url}}/api/employees/e2e-manager-clocks-employee/clocks?page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 2
jsonpath "$.entries[0].clockEvent.employeeId" == "e2e-manager-clocks-employee"
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
GET {{base_url}}/api/sites/e2e-manager-clocks-site/clocks?page=1&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T07:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "ACCEPTED"
jsonpath "$.page" == 1
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# The historical team includes the rejected attempt; the current team does not.
GET {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-historical-team/clocks?page=0&size=2
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
GET {{base_url}}/api/employees/e2e-manager-clocks-employee/clocks?status=REJECTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "REJECTED"
jsonpath "$.entries[0].validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 1
jsonpath "$.totalPages" == 1

# Status filtering preserves the site page metadata and returns only accepted attempts.
GET {{base_url}}/api/sites/e2e-manager-clocks-site/clocks?status=ACCEPTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 2
jsonpath "$.entries[0].clockEvent.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T09:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "ACCEPTED"
jsonpath "$.entries[0].validationResult.siteId" == "e2e-manager-clocks-site"
jsonpath "$.entries[1].clockEvent.timestamp" == "2026-08-16T07:00:00Z"
jsonpath "$.entries[1].validationResult.decision" == "ACCEPTED"
jsonpath "$.entries[1].validationResult.siteId" == "e2e-manager-clocks-site"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 2
jsonpath "$.totalPages" == 1

# Status filtering preserves the team page metadata and returns only rejected attempts.
GET {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-historical-team/clocks?status=REJECTED&page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].clockEvent.employeeId" == "e2e-manager-clocks-employee"
jsonpath "$.entries[0].clockEvent.timestamp" == "2026-08-16T08:00:00Z"
jsonpath "$.entries[0].validationResult.decision" == "REJECTED"
jsonpath "$.entries[0].validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 1
jsonpath "$.totalPages" == 1

GET {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-current-team/clocks?page=0&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 0
jsonpath "$.page" == 0
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 0
jsonpath "$.totalPages" == 0

# A page beyond the result set is valid and returns no entries.
GET {{base_url}}/api/employees/e2e-manager-clocks-employee/clocks?page=3&size=2
HTTP 200
[Asserts]
jsonpath "$.entries" count == 0
jsonpath "$.page" == 3
jsonpath "$.size" == 2
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 2

# Invalid page sizes are rejected before lookup.
GET {{base_url}}/api/employees/e2e-manager-clocks-employee/clocks?size=0
HTTP 400

# Unknown employee, site, and team resources return 404.
GET {{base_url}}/api/employees/e2e-manager-clocks-unknown-employee/clocks?page=0&size=2
HTTP 404

GET {{base_url}}/api/sites/e2e-manager-clocks-unknown-site/clocks?page=0&size=2
HTTP 404

GET {{base_url}}/api/sites/e2e-manager-clocks-site/teams/e2e-manager-clocks-unknown-team/clocks?page=0&size=2
HTTP 404
