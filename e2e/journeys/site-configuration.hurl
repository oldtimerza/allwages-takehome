# Create the site that owns the journey's configuration.
POST {{base_url}}/api/sites
Content-Type: application/json
{
  "name": "E2E Farm",
  "validationRules": null
}
HTTP 201
[Captures]
site_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Farm"

# Register the employee before assigning them to the site team.
POST {{base_url}}/api/employees
Content-Type: application/json
{
  "name": "E2E Employee",
  "phoneNumber": "+27115550100"
}
HTTP 201
[Captures]
employee_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Employee"
jsonpath "$.phoneNumber" == "+27115550100"

# Configure the primary location where the team works.
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

# Add the team and assign the registered employee to it.
POST {{base_url}}/api/sites/{{site_id}}/teams
Content-Type: application/json
{
  "name": "E2E Harvest Team",
  "validationRules": null
}
HTTP 201
[Captures]
team_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" matches /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
jsonpath "$.name" == "E2E Harvest Team"

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

# Verify independent validation-rule configuration at every supported scope.
PUT {{base_url}}/api/sites/{{site_id}}/validation-rules
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

PUT {{base_url}}/api/sites/{{site_id}}/teams/{{team_id}}/validation-rules
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

PUT {{base_url}}/api/sites/{{site_id}}/employees/{{employee_id}}/validation-rules
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
