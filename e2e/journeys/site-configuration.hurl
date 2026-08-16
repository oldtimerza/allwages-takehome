# Create the site that owns the journey's configuration.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "id": "e2e-site-configuration",
  "name": "E2E Farm",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-site-configuration"
jsonpath "$.name" == "E2E Farm"

# Register the employee before assigning them to the site team.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-employee-configuration",
  "name": "E2E Employee",
  "phoneNumber": "+27115550100"
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-employee-configuration"
jsonpath "$.name" == "E2E Employee"
jsonpath "$.phoneNumber" == "+27115550100"

# The employee identifier remains unique for the life of the application.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "id": "e2e-employee-configuration",
  "name": "Duplicate E2E Employee",
  "phoneNumber": "+27115550101"
}
HTTP 409

# Configure the primary location where the team works.
POST {{base_url}}/api/sites/e2e-site-configuration/geofences
Content-Type: application/json
{
  "id": "e2e-primary-zone",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "radiusMeters": 100,
  "primary": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-primary-zone"
jsonpath "$.primary" == true

# Add the team and assign the registered employee to it.
POST {{base_url}}/api/sites/e2e-site-configuration/teams
Content-Type: application/json
{
  "id": "e2e-harvest-team",
  "name": "E2E Harvest Team",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.id" == "e2e-harvest-team"
jsonpath "$.name" == "E2E Harvest Team"

POST {{base_url}}/api/sites/e2e-site-configuration/teams/e2e-harvest-team/assignments
Content-Type: application/json
{
  "employeeId": "e2e-employee-configuration",
  "effectiveFrom": "2026-01-01",
  "validationRules": null
}
HTTP 201
[Asserts]
jsonpath "$.employeeId" == "e2e-employee-configuration"
jsonpath "$.teamId" == "e2e-harvest-team"

# Verify independent validation-rule configuration at every supported scope.
PUT {{base_url}}/api/sites/e2e-site-configuration/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 50,
  "approvalRequired": false,
  "strictModeHours": [
    {
      "days": ["MONDAY"],
      "startsAt": "08:00",
      "endsAt": "17:00",
      "toleranceMeters": 10
    }
  ]
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 50
jsonpath "$.approvalRequired" == false
jsonpath "$.strictModeHours[0].toleranceMeters" == 10

PUT {{base_url}}/api/sites/e2e-site-configuration/teams/e2e-harvest-team/validation-rules
Content-Type: application/json
{
  "toleranceMeters": 40,
  "approvalRequired": true,
  "strictModeHours": null
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 40
jsonpath "$.approvalRequired" == true

PUT {{base_url}}/api/sites/e2e-site-configuration/employees/e2e-employee-configuration/validation-rules
Content-Type: application/json
{
  "effectiveFrom": "2026-01-01",
  "validationRules": {
    "toleranceMeters": 30,
    "approvalRequired": true,
    "strictModeHours": null
  }
}
HTTP 200
[Asserts]
jsonpath "$.toleranceMeters" == 30
jsonpath "$.approvalRequired" == true
