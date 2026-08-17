# Establish an independently configured employee who can submit clock attempts.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "id": "e2e-audit-events-site",
  "name": "E2E Audit Events Farm",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-audit-events-site"

POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-audit-events-employee",
  "name": "E2E Audit Events Employee",
  "phoneNumber": "+27115550120"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-audit-events-employee"

POST {{base_url}}/api/sites/e2e-audit-events-site/geofences
Content-Type: application/json
{
  "id": "e2e-audit-events-primary-zone",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "radiusMeters": 100,
  "primary": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-audit-events-primary-zone"
jsonpath "$.primary" == true

POST {{base_url}}/api/sites/e2e-audit-events-site/teams
Content-Type: application/json
{
  "id": "e2e-audit-events-team",
  "name": "E2E Audit Events Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-audit-events-team"

POST {{base_url}}/api/sites/e2e-audit-events-site/teams/e2e-audit-events-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-audit-events-employee",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-audit-events-employee"
jsonpath "$.teamId" == "e2e-audit-events-team"

# Submit an accepted attempt first.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-audit-events-employee",
  "timestamp": "2026-08-16T09:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "ACCEPTED"

# A later request outside the geofence is rejected and should be the newest audit event.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-audit-events-employee",
  "timestamp": "2026-08-16T09:01:00+02:00",
  "latitude": -26.2141,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "OUT"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "REJECTED"
jsonpath "$.validationResult.reason" == "OUTSIDE_GEOFENCE"

# A future in-zone request is rejected after the outside-geofence attempt.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-audit-events-employee",
  "timestamp": "2099-08-16T09:02:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "REJECTED"
jsonpath "$.validationResult.reason" == "FUTURE_TIMESTAMP"

# Page zero contains only the newest future-timestamp rejection.
GET {{base_url}}/api/audit-events?page=0&size=1
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].type" == "CLOCK_REJECTED"
jsonpath "$.entries[0].employeeId" == "e2e-audit-events-employee"
jsonpath "$.entries[0].reasonCode" == "FUTURE_TIMESTAMP"
jsonpath "$.entries[0].source" == "MOBILE_API"
jsonpath "$.entries[0].httpStatus" == 200
jsonpath "$.page" == 0
jsonpath "$.size" == 1
jsonpath "$.totalElements" == 3
jsonpath "$.totalPages" == 3

# Filtering returns the newest rejected attempt and matching page metadata.
GET {{base_url}}/api/audit-events?page=0&size=1&type=CLOCK_REJECTED
HTTP 200
[Asserts]
jsonpath "$.entries" count == 1
jsonpath "$.entries[0].type" == "CLOCK_REJECTED"
jsonpath "$.entries[0].reasonCode" == "FUTURE_TIMESTAMP"
jsonpath "$.entries[0].source" == "MOBILE_API"
jsonpath "$.entries[0].httpStatus" == 200
jsonpath "$.page" == 0
jsonpath "$.size" == 1
jsonpath "$.totalElements" == 2
jsonpath "$.totalPages" == 2
