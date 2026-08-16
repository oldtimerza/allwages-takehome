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
