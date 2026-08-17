# Create the site and employee required for an eligible clock event.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "id": "e2e-clock-validation-site",
  "name": "E2E Clock Validation Farm",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-site"

POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-clock-validation-employee",
  "name": "E2E Clock Validation Employee",
  "phoneNumber": "+27115550110"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-employee"

# An effective primary geofence contains the coordinates used by the clock.
POST {{base_url}}/api/sites/e2e-clock-validation-site/geofences
Content-Type: application/json
{
  "id": "e2e-clock-validation-primary-zone",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "radiusMeters": 100,
  "primary": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-primary-zone"
jsonpath "$.primary" == true

# Assign the employee to a site team before submitting their clock.
POST {{base_url}}/api/sites/e2e-clock-validation-site/teams
Content-Type: application/json
{
  "id": "e2e-clock-validation-team",
  "name": "E2E Clock Validation Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-team"

POST {{base_url}}/api/sites/e2e-clock-validation-site/teams/e2e-clock-validation-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-employee",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-clock-validation-employee"
jsonpath "$.teamId" == "e2e-clock-validation-team"

# Set the site baseline before more-specific team and employee overrides are applied.
PUT {{base_url}}/api/sites/e2e-clock-validation-site/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 20,
  "approvalRequired": null,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 20

# A future clock is rejected before site or geofence validation, even at in-zone coordinates.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-employee",
  "timestamp": "2099-01-01T09:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "REJECTED"
jsonpath "$.validationResult.reason" == "FUTURE_TIMESTAMP"
jsonpath "$.validationResult.siteId" == null
jsonpath "$.validationResult.geofenceId" == null

# The employee clocks in at the centre of the effective primary geofence.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-employee",
  "timestamp": "2026-08-16T09:00:00+02:00",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "ACCEPTED"
jsonpath "$.validationResult.reason" == "ACCEPTED"
jsonpath "$.validationResult.siteId" == "e2e-clock-validation-site"
jsonpath "$.validationResult.geofenceId" == "e2e-clock-validation-primary-zone"

# A team-level tolerance of 150 m overrides the site's 20 m tolerance.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-clock-validation-team-employee",
  "name": "E2E Team Override Employee",
  "phoneNumber": "+27115550111"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-team-employee"

POST {{base_url}}/api/sites/e2e-clock-validation-site/teams
Content-Type: application/json
{
  "id": "e2e-clock-validation-permissive-team",
  "name": "E2E Permissive Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-permissive-team"

POST {{base_url}}/api/sites/e2e-clock-validation-site/teams/e2e-clock-validation-permissive-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-team-employee",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-clock-validation-team-employee"
jsonpath "$.teamId" == "e2e-clock-validation-permissive-team"

PUT {{base_url}}/api/sites/e2e-clock-validation-site/teams/e2e-clock-validation-permissive-team/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 150,
  "approvalRequired": null,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 150

# This point is about 200 m from the centre: outside 100 m + site 20 m, but inside 100 m + team 150 m.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-team-employee",
  "timestamp": "2026-08-16T09:01:00+02:00",
  "latitude": -26.2059,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "ACCEPTED"
jsonpath "$.validationResult.reason" == "ACCEPTED"
jsonpath "$.validationResult.siteId" == "e2e-clock-validation-site"
jsonpath "$.validationResult.geofenceId" == "e2e-clock-validation-primary-zone"

# An employee-level tolerance of 10 m overrides this team's 150 m tolerance.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-clock-validation-employee-override",
  "name": "E2E Employee Override Employee",
  "phoneNumber": "+27115550112"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-employee-override"

POST {{base_url}}/api/sites/e2e-clock-validation-site/teams
Content-Type: application/json
{
  "id": "e2e-clock-validation-strict-team",
  "name": "E2E Strict Employee Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-clock-validation-strict-team"

POST {{base_url}}/api/sites/e2e-clock-validation-site/teams/e2e-clock-validation-strict-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-employee-override",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-clock-validation-employee-override"
jsonpath "$.teamId" == "e2e-clock-validation-strict-team"

PUT {{base_url}}/api/sites/e2e-clock-validation-site/teams/e2e-clock-validation-strict-team/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 150,
  "approvalRequired": null,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 150

PUT {{base_url}}/api/sites/e2e-clock-validation-site/employees/e2e-clock-validation-employee-override/validation-rules
Content-Type: application/json
{
  "effectiveFrom": "2026-01-01",
  "validationRules": {
    "toleranceMeters": 10,
    "approvalRequired": null,
    "strictModeHours": null
  }
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 10

# The same point is outside 100 m + employee 10 m even though it is inside the team's 150 m limit.
POST {{base_url}}/api/clocks
Content-Type: application/json
{
  "employeeId": "e2e-clock-validation-employee-override",
  "timestamp": "2026-08-16T09:02:00+02:00",
  "latitude": -26.2059,
  "longitude": 28.0473,
  "accuracyMeters": 5,
  "type": "IN"
}
HTTP 200
[Asserts]
jsonpath "$.validationResult.decision" == "REJECTED"
jsonpath "$.validationResult.reason" == "OUTSIDE_GEOFENCE"
jsonpath "$.validationResult.siteId" == null
jsonpath "$.validationResult.geofenceId" == null
