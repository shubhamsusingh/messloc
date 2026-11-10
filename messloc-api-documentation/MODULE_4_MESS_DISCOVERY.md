# MessLoc – Module 4: Public Mess Discovery API Specification

## 1. Overview & Architectural Principles

Module 4 provides public, unauthenticated and authenticated discovery of mess facilities on the MessLoc platform.

### Core Visibility Rule:
A mess listing is **only** publicly discoverable when **both** of the following conditions are met:
1. `verificationStatus == VerificationStatus.APPROVED` (administrative verification passed).
2. `status == MessStatus.ACTIVE` (operational status enabled by the owner).

Any listing with `verificationStatus = PENDING`, `verificationStatus = REJECTED`, or `status = INACTIVE` is strictly filtered out at the MongoDB database level and will return HTTP 404 if accessed by ID, preventing enumeration attacks or exposure of pending data.

### Privacy & Data Protection:
All discovery endpoints return dedicated public response DTOs (`PublicMessResponse` and `MessDetailsResponse`). Internal sensitive attributes—such as `ownerId`, administrative verification audit trails, status flags, and timestamps—are never leaked to public consumers.

### Geolocation Strategy:
To maintain backward compatibility with existing documents stored with separate `latitude` (Double) and `longitude` (Double) fields (without forcing destructive schema migrations or complex GeoJSON restructuring), nearby discovery employs high-precision **Haversine spherical distance calculations** ($R = 6371\text{ km}$). Pre-filtering queries MongoDB directly for active/approved records, computes distance in kilometres rounded to 2 decimal places, bounds results within the client's radius, and sorts nearest-first.

---

## 2. Implemented Endpoints Reference

### 2.1 Get All Approved Messes
- **Endpoint:** `GET /api/messes`
- **Authentication:** `Public (None required)`
- **Query Parameters:** None
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<List<PublicMessResponse>>`

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes" \
  -H "Accept: application/json"
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Messes fetched successfully",
  "data": [
    {
      "id": "675abc123456789012345678",
      "name": "Annapurna Mess",
      "description": "Pure vegetarian homely meals",
      "address": "Rajpur Road, Near Clock Tower",
      "city": "Dehradun",
      "state": "Uttarakhand",
      "pincode": "248001",
      "latitude": 30.3255,
      "longitude": 78.0436,
      "contactNumber": "9876543210",
      "monthlyPrice": 3200.0,
      "foodType": "VEG",
      "openingTime": "08:00",
      "closingTime": "22:00",
      "facilities": [
        "WiFi",
        "RO Water",
        "Parking"
      ]
    }
  ],
  "timestamp": "2026-11-10T10:00:00"
}
```

---

### 2.2 Search Messes by City
- **Endpoint:** `GET /api/messes?city={city}`
- **Authentication:** `Public (None required)`
- **Query Parameters:**
  - `city` (string, optional): Target city name. Case-insensitive and trimmed.
- **Success Code:** `200 OK`
- **Validation Rules:** Blank strings (`?city=   `) return `400 Bad Request`.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes?city=Dehradun" \
  -H "Accept: application/json"
```

#### Empty Matches Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Messes fetched successfully",
  "data": [],
  "timestamp": "2026-11-10T10:01:00"
}
```

#### Blank Parameter Error (400 Bad Request):
```json
{
  "success": false,
  "status": 400,
  "message": "City search parameter cannot be blank",
  "timestamp": "2026-11-10T10:02:00"
}
```

---

### 2.3 Filter Messes by Food Type
- **Endpoint:** `GET /api/messes?foodType={foodType}`
- **Authentication:** `Public (None required)`
- **Supported Values:** `VEG`, `NON_VEG`, `BOTH` (case-insensitive, e.g., `veg`, `Non_Veg`, `both`).
- **Domain Business Rules:**
  - Selecting `VEG` queries for listings offering vegetarian meals: matches both `VEG` and `BOTH` establishments.
  - Selecting `NON_VEG` queries for listings offering non-vegetarian meals: matches both `NON_VEG` and `BOTH` establishments.
  - Selecting `BOTH` queries specifically for establishments offering both options.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes?foodType=VEG" \
  -H "Accept: application/json"
```

#### Invalid Food Type Error (400 Bad Request):
```json
{
  "success": false,
  "status": 400,
  "message": "Invalid foodType: 'FAST_FOOD'. Allowed values: VEG, NON_VEG, BOTH",
  "timestamp": "2026-11-10T10:03:00"
}
```

---

### 2.4 Combined City and Food Type Filters
- **Endpoint:** `GET /api/messes?city={city}&foodType={foodType}`
- **Authentication:** `Public (None required)`
- **Behavior:** Executes a compound MongoDB query with indexed fields `city` and `foodType` under `verificationStatus=APPROVED` and `status=ACTIVE`.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes?city=Dehradun&foodType=NON_VEG" \
  -H "Accept: application/json"
```

---

### 2.5 Get Mess Details by ID
- **Endpoint:** `GET /api/messes/{id}`
- **Authentication:** `Public (None required)`
- **Path Variable:** `id` (string, required): MongoDB ObjectID / identifier.
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<MessDetailsResponse>`
- **Security Check:** Returns `404 Not Found` if listing does not exist OR is `PENDING`, `REJECTED`, or `INACTIVE`.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes/675abc123456789012345678" \
  -H "Accept: application/json"
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Mess fetched successfully",
  "data": {
    "id": "675abc123456789012345678",
    "name": "Annapurna Mess",
    "description": "Pure vegetarian homely meals",
    "address": "Rajpur Road, Near Clock Tower",
    "city": "Dehradun",
    "state": "Uttarakhand",
    "pincode": "248001",
    "latitude": 30.3255,
    "longitude": 78.0436,
    "contactNumber": "9876543210",
    "monthlyPrice": 3200.0,
    "foodType": "VEG",
    "openingTime": "08:00",
    "closingTime": "22:00",
    "facilities": [
      "WiFi",
      "RO Water",
      "Parking"
    ]
  },
  "timestamp": "2026-11-10T10:04:00"
}
```

#### Unapproved / Not Found Error (404 Not Found):
```json
{
  "success": false,
  "status": 404,
  "message": "Mess is not publicly available or pending verification",
  "timestamp": "2026-11-10T10:05:00"
}
```

---

### 2.6 Nearby Mess Discovery
- **Endpoint:** `GET /api/messes/nearby?latitude={lat}&longitude={lng}&radius={km}`
- **Authentication:** `Public (None required)`
- **Query Parameters:**
  - `latitude` (double, required): Target user latitude `[-90.0 to 90.0]`.
  - `longitude` (double, required): Target user longitude `[-180.0 to 180.0]`.
  - `radius` (double, optional, default: `5.0`): Search radius in kilometres `(0.0 < radius <= 100.0)`.
  - `foodType` (string, optional): Optional dietary filter (`VEG`, `NON_VEG`, `BOTH`).
- **Success Code:** `200 OK`
- **Response Feature:** Response items include `distanceInKm` (rounded to 2 decimal places) and are sorted **nearest-first**.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/messes/nearby?latitude=30.3165&longitude=78.0322&radius=5" \
  -H "Accept: application/json"
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Nearby messes fetched successfully",
  "data": [
    {
      "id": "675abc123456789012345678",
      "name": "Annapurna Mess",
      "description": "Pure vegetarian homely meals",
      "address": "Rajpur Road, Near Clock Tower",
      "city": "Dehradun",
      "state": "Uttarakhand",
      "pincode": "248001",
      "latitude": 30.3255,
      "longitude": 78.0436,
      "contactNumber": "9876543210",
      "monthlyPrice": 3200.0,
      "foodType": "VEG",
      "openingTime": "08:00",
      "closingTime": "22:00",
      "facilities": [
        "WiFi",
        "RO Water"
      ],
      "distanceInKm": 1.45
    }
  ],
  "timestamp": "2026-11-10T10:06:00"
}
```

#### Out of Range Latitude Error (400 Bad Request):
```json
{
  "success": false,
  "status": 400,
  "message": "Latitude must be between -90 and 90",
  "timestamp": "2026-11-10T10:07:00"
}
```

#### Missing Parameter Error (400 Bad Request):
```json
{
  "success": false,
  "status": 400,
  "message": "Required parameter 'latitude' is missing",
  "timestamp": "2026-11-10T10:08:00"
}
```

