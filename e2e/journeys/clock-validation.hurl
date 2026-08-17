# Create the site and employee required for an eligible clock event.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "name": "E2E Clock Validation Farm",
  "validationRules": null
}
HTTP 201
[Captures]
site_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/employees
Content-Type: application/json
{
  "name": "E2E Clock Validation Employee",
  "phoneNumber": "+27115550110"
}
HTTP 201
[Captures]
employee_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

# An effective primary geofence contains the coordinates used by the clock.
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

# Assign the employee to a site team before submitting their clock.
POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Clock Validation Team",
  "validationRules": null
}
HTTP 201
[Captures]
team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/sites/{{site_id}}/teams/{{team_id}}/assignments
Content-Type: application/json
{
  "employeeId": "{{employee_id}}",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "{{employee_id}}"
jsonpath "$.teamId" == "{{team_id}}"

# Set the site baseline before more-specific team and employee overrides are applied.
PUT {{base_url}}/api/sites/{{site_id}}/validation-rules
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
  "employeeId": "{{employee_id}}",
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
  "employeeId": "{{employee_id}}",
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
jsonpath "$.validationResult.siteId" == "{{site_id}}"
jsonpath "$.validationResult.geofenceId" == "{{geofence_id}}"

# A team-level tolerance of 150 m overrides the site's 20 m tolerance.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "name": "E2E Team Override Employee",
  "phoneNumber": "+27115550111"
}
HTTP 201
[Captures]
team_employee_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Permissive Team",
  "validationRules": null
}
HTTP 201
[Captures]
permissive_team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/sites/{{site_id}}/teams/{{permissive_team_id}}/assignments
Content-Type: application/json
{
  "employeeId": "{{team_employee_id}}",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "{{team_employee_id}}"
jsonpath "$.teamId" == "{{permissive_team_id}}"

PUT {{base_url}}/api/sites/{{site_id}}/teams/{{permissive_team_id}}/validation-rules
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
  "employeeId": "{{team_employee_id}}",
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
jsonpath "$.validationResult.siteId" == "{{site_id}}"
jsonpath "$.validationResult.geofenceId" == "{{geofence_id}}"

# An employee-level tolerance of 10 m overrides this team's 150 m tolerance.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "name": "E2E Employee Override Employee",
  "phoneNumber": "+27115550112"
}
HTTP 201
[Captures]
employee_override_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Strict Employee Team",
  "validationRules": null
}
HTTP 201
[Captures]
strict_team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

POST {{base_url}}/api/sites/{{site_id}}/teams/{{strict_team_id}}/assignments
Content-Type: application/json
{
  "employeeId": "{{employee_override_id}}",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "{{employee_override_id}}"
jsonpath "$.teamId" == "{{strict_team_id}}"

PUT {{base_url}}/api/sites/{{site_id}}/teams/{{strict_team_id}}/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 150,
  "approvalRequired": null,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 150

PUT {{base_url}}/api/sites/{{site_id}}/employees/{{employee_override_id}}/validation-rules
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
  "employeeId": "{{employee_override_id}}",
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
