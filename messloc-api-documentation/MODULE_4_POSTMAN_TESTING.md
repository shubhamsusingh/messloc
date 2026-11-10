# Module 4: Public Mess Discovery – Postman Testing Guide

This guide details testing procedures for verifying all Public Mess Discovery endpoints in Postman.

---

## 1. Prerequisites & Environment Setup

Configure your Postman environment or Collection Variables:
- `baseUrl`: `http://localhost:8080`
- `messId`: (auto-captured from approved mess responses or seeded ID)

> [!NOTE]
> All discovery endpoints (`GET /api/messes/**`) are completely **public** and do **NOT** require an `Authorization` header. You can execute them with or without an active JWT token.

---

## 2. Test Execution Workflow

### Step 1: Browse All Approved Messes
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes`
- **Headers:** None required
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response envelope is valid and data is array", function () {
    const json = pm.response.json();
    pm.expect(json.success).to.be.true;
    pm.expect(json.data).to.be.an("array");
    if (json.data.length > 0) {
        // Save first mess ID for subsequent tests
        pm.collectionVariables.set("messId", json.data[0].id);
        // Ensure sensitive fields are not leaked
        pm.expect(json.data[0]).to.not.have.property("ownerId");
        pm.expect(json.data[0]).to.not.have.property("verificationStatus");
    }
});
```

---

### Step 2: City Search
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes?city=Dehradun`
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("All returned messes belong to searched city", function () {
    const json = pm.response.json();
    json.data.forEach(function (mess) {
        pm.expect(mess.city.toLowerCase()).to.eql("dehradun");
    });
});
```

---

### Step 3: Food Type Filtering (VEG)
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes?foodType=VEG`
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Returned messes are either VEG or BOTH", function () {
    const json = pm.response.json();
    json.data.forEach(function (mess) {
        pm.expect(["VEG", "BOTH"]).to.include(mess.foodType);
    });
});
```

---

### Step 4: Combined Filter (City + Food Type)
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes?city=Dehradun&foodType=VEG`
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Matches both city and dietary criteria", function () {
    const json = pm.response.json();
    json.data.forEach(function (mess) {
        pm.expect(mess.city.toLowerCase()).to.eql("dehradun");
        pm.expect(["VEG", "BOTH"]).to.include(mess.foodType);
    });
});
```

---

### Step 5: Get Public Mess Details by ID
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes/{{messId}}`
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Returns full public details without owner identity", function () {
    const json = pm.response.json();
    pm.expect(json.success).to.be.true;
    pm.expect(json.data.id).to.eql(pm.collectionVariables.get("messId"));
    pm.expect(json.data.name).to.be.a("string");
    pm.expect(json.data.address).to.be.a("string");
    pm.expect(json.data).to.not.have.property("ownerId");
});
```

---

### Step 6: Nearby Discovery
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/messes/nearby?latitude=30.3165&longitude=78.0322&radius=5`
- **Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Returns messes sorted nearest first within 5 km", function () {
    const json = pm.response.json();
    let prevDistance = -1;
    json.data.forEach(function (mess) {
        pm.expect(mess.distanceInKm).to.be.a("number");
        pm.expect(mess.distanceInKm).to.be.at.most(5.0);
        pm.expect(mess.distanceInKm).to.be.at.least(prevDistance);
        prevDistance = mess.distanceInKm;
    });
});
```

---

## 3. Negative and Boundary Test Scenarios

| Scenario | Request | Expected Status | Expected Error Message |
| :--- | :--- | :---: | :--- |
| **Invalid Food Type** | `GET /api/messes?foodType=PIZZA` | `400 Bad Request` | `Invalid foodType: 'PIZZA'. Allowed values: VEG, NON_VEG, BOTH` |
| **Blank City** | `GET /api/messes?city=   ` | `400 Bad Request` | `City search parameter cannot be blank` |
| **Out-of-bound Lat** | `GET /api/messes/nearby?latitude=95.0&longitude=78.0` | `400 Bad Request` | `Latitude must be between -90 and 90` |
| **Out-of-bound Long** | `GET /api/messes/nearby?latitude=30.0&longitude=190.0` | `400 Bad Request` | `Longitude must be between -180 and 180` |
| **Zero/Neg Radius** | `GET /api/messes/nearby?latitude=30.0&longitude=78.0&radius=0` | `400 Bad Request` | `Radius must be greater than 0` |
| **Excessive Radius** | `GET /api/messes/nearby?latitude=30.0&longitude=78.0&radius=150` | `400 Bad Request` | `Radius cannot exceed 100 km` |
| **Missing Latitude** | `GET /api/messes/nearby?longitude=78.0` | `400 Bad Request` | `Required parameter 'latitude' is missing` |
| **Non-existent ID** | `GET /api/messes/675abc999999999999999999` | `404 Not Found` | `Mess not found with id: ...` |
| **Pending Mess ID** | `GET /api/messes/{pendingMessId}` | `404 Not Found` | `Mess is not publicly available or pending verification` |
| **Inactive Mess ID** | `GET /api/messes/{inactiveMessId}` | `404 Not Found` | `Mess is not publicly available or pending verification` |
| **Unauth Mess Add** | `POST /api/messes` without token | `401 Unauthorized` | Access denied / Full authentication is required |
| **User Role Add** | `POST /api/messes` with `ROLE_USER` | `403 Forbidden` | Access denied: You do not have permission |

