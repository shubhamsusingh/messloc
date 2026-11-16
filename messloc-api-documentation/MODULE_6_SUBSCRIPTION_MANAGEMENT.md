# MessLoc – Module 6: Subscription Management API Specification

## 1. Overview & Architectural Principles

Module 6 implements secure, multi-tenant, and role-based **Subscription Management** for the MessLoc platform.

### Core Business Rules & Invariants:
1. **Customer-Driven Subscriptions**:
   Only authenticated users with role `USER` whose accounts are in `ACTIVE` status can create or cancel subscriptions. Owners and administrators cannot subscribe to messes.
2. **Mess Eligibility Gate**:
   A user can subscribe only to an existing mess that is both verified (`verificationStatus == APPROVED`) and operational (`status == ACTIVE`). Any attempt to subscribe to a pending, rejected, or inactive mess is rejected with HTTP 400 Bad Request.
3. **Duplicate Subscription Prevention**:
   A user can have at most one `ACTIVE` subscription to a specific mess at any given time. If an active subscription already exists, attempting to create a new one returns HTTP 409 Conflict. If a prior subscription has already expired (`endDate < today`) or was cancelled, the user is permitted to create a new subscription.
4. **Backend-Generated Dates & Plan Calculation**:
   Subscription dates are strictly computed on the backend and are never accepted from the frontend. `startDate` defaults to the current date (`LocalDate.now()`), and `endDate` is calculated from the plan's duration (`startDate.plusDays(durationDays)`).
5. **Subscription Plans**:
   - `monthly` (default): 30 days, priced at the mess's monthly price.
   - `quarterly`: 90 days, priced at 3x monthly price.
   - `half-yearly`: 180 days, priced at 6x monthly price.
   - `yearly`: 365 days, priced at 12x monthly price.
   - Custom plans: support custom `planName` and positive `durationDays`.
6. **Non-Destructive Cancellation**:
   When a subscriber cancels an active subscription, the subscription document is not deleted. Its status is updated to `CANCELLED`, and `cancelledAt` is recorded. Already cancelled or expired subscriptions cannot be cancelled again.
7. **Strict Ownership Enforcement (IDOR Prevention)**:
   - A customer can view and cancel only their own subscriptions.
   - A mess owner can view subscriptions associated only with messes that they own.
   - An administrator has platform-wide visibility across all subscriptions.

---

## 2. API Endpoints Reference

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| `POST` | `/api/subscriptions` | `USER` | Subscribe to an approved and active mess facility |
| `GET` | `/api/subscriptions/me` | `USER` | Get all subscriptions belonging to current customer |
| `GET` | `/api/subscriptions/{subscriptionId}` | `USER`, `ADMIN` | View detailed subscription information |
| `PATCH` | `/api/subscriptions/{subscriptionId}/cancel` | `USER` | Cancel customer's own active subscription |
| `GET` | `/api/subscriptions/owner/mess/{messId}` | `OWNER` | View subscriptions and subscriber details for owner's mess |
| `GET` | `/api/admin/subscriptions` | `ADMIN` | Platform-wide subscription audit and monitoring |

---

### 2.1 Subscribe to a Mess
- **Endpoint:** `POST /api/subscriptions`
- **Authentication:** `Bearer <JWT_TOKEN>` (`USER` role required)
- **Success Code:** `201 Created`
- **Response Envelope:** `ApiResponse<SubscriptionResponse>`

#### Request Body:
```json
{
  "messId": "67320b98f1234567890abcde",
  "planId": "monthly"
}
```

#### Optional Plan Selection:
```json
{
  "messId": "67320b98f1234567890abcde",
  "planId": "quarterly"
}
```

#### Success Response (201 Created):
```json
{
  "success": true,
  "status": 201,
  "message": "Subscription created successfully",
  "data": {
    "id": "6733a1b2c3d4e5f678901234",
    "userId": "67320a11f1234567890abcde",
    "userName": "Rohan Sharma",
    "userEmail": "rohan.sharma@example.com",
    "userPhone": "9876543210",
    "messId": "67320b98f1234567890abcde",
    "messName": "Shree Krishna Bhojanalaya",
    "planId": "monthly",
    "planName": "Monthly Plan",
    "durationDays": 30,
    "price": 3200.0,
    "startDate": "2026-11-12",
    "endDate": "2026-12-12",
    "status": "ACTIVE",
    "createdAt": "2026-11-12T18:00:00Z",
    "updatedAt": "2026-11-12T18:00:00Z"
  },
  "timestamp": "2026-11-12T23:30:00"
}
```

#### Error Responses:
- `400 Bad Request`: Mess is not approved or inactive, or duration <= 0.
- `401 Unauthorized`: Token missing or invalid.
- `403 Forbidden`: Account is blocked, inactive, or role is not `USER`.
- `404 Not Found`: Mess not found.
- `409 Conflict`: User already has an active subscription to this mess.

---

### 2.2 Get My Subscriptions
- **Endpoint:** `GET /api/subscriptions/me`
- **Authentication:** `Bearer <JWT_TOKEN>` (`USER` role required)
- **Query Parameters:** `status` (optional: `ACTIVE`, `CANCELLED`, `EXPIRED`)
- **Success Code:** `200 OK`

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Subscriptions fetched successfully",
  "data": [
    {
      "id": "6733a1b2c3d4e5f678901234",
      "userId": "67320a11f1234567890abcde",
      "messId": "67320b98f1234567890abcde",
      "messName": "Shree Krishna Bhojanalaya",
      "planId": "monthly",
      "planName": "Monthly Plan",
      "durationDays": 30,
      "price": 3200.0,
      "startDate": "2026-11-12",
      "endDate": "2026-12-12",
      "status": "ACTIVE",
      "createdAt": "2026-11-12T18:00:00Z",
      "updatedAt": "2026-11-12T18:00:00Z"
    }
  ],
  "timestamp": "2026-11-12T23:31:00"
}
```

---

### 2.3 Get Subscription Details by ID
- **Endpoint:** `GET /api/subscriptions/{subscriptionId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`USER` subscriber or `ADMIN`)
- **Path Variable:** `subscriptionId`
- **Success Code:** `200 OK`

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Subscription retrieved successfully",
  "data": {
    "id": "6733a1b2c3d4e5f678901234",
    "userId": "67320a11f1234567890abcde",
    "userName": "Rohan Sharma",
    "userEmail": "rohan.sharma@example.com",
    "userPhone": "9876543210",
    "messId": "67320b98f1234567890abcde",
    "messName": "Shree Krishna Bhojanalaya",
    "planId": "monthly",
    "planName": "Monthly Plan",
    "durationDays": 30,
    "price": 3200.0,
    "startDate": "2026-11-12",
    "endDate": "2026-12-12",
    "status": "ACTIVE",
    "createdAt": "2026-11-12T18:00:00Z",
    "updatedAt": "2026-11-12T18:00:00Z"
  },
  "timestamp": "2026-11-12T23:32:00"
}
```

#### Error Responses:
- `403 Forbidden`: User attempts to view a subscription belonging to someone else.
- `404 Not Found`: Subscription not found.

---

### 2.4 Cancel Active Subscription
- **Endpoint:** `PATCH /api/subscriptions/{subscriptionId}/cancel`
- **Authentication:** `Bearer <JWT_TOKEN>` (`USER` role required)
- **Path Variable:** `subscriptionId`
- **Success Code:** `200 OK`

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Subscription cancelled successfully",
  "data": {
    "id": "6733a1b2c3d4e5f678901234",
    "userId": "67320a11f1234567890abcde",
    "userName": "Rohan Sharma",
    "userEmail": "rohan.sharma@example.com",
    "userPhone": "9876543210",
    "messId": "67320b98f1234567890abcde",
    "messName": "Shree Krishna Bhojanalaya",
    "planId": "monthly",
    "planName": "Monthly Plan",
    "durationDays": 30,
    "price": 3200.0,
    "startDate": "2026-11-12",
    "endDate": "2026-12-12",
    "status": "CANCELLED",
    "createdAt": "2026-11-12T18:00:00Z",
    "updatedAt": "2026-11-12T18:05:00Z",
    "cancelledAt": "2026-11-12T18:05:00Z"
  },
  "timestamp": "2026-11-12T23:35:00"
}
```

#### Error Responses:
- `400 Bad Request`: Subscription is already cancelled or expired.
- `403 Forbidden`: User attempts to cancel another customer's subscription.
- `404 Not Found`: Subscription not found.

---

### 2.5 Get Subscriptions for Owner's Mess
- **Endpoint:** `GET /api/subscriptions/owner/mess/{messId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` role required)
- **Path Variable:** `messId`
- **Query Parameters:** `status` (optional: `ACTIVE`, `CANCELLED`, `EXPIRED`)
- **Success Code:** `200 OK`

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "Owner mess subscriptions fetched successfully",
  "data": [
    {
      "id": "6733a1b2c3d4e5f678901234",
      "userId": "67320a11f1234567890abcde",
      "userName": "Rohan Sharma",
      "userEmail": "rohan.sharma@example.com",
      "userPhone": "9876543210",
      "messId": "67320b98f1234567890abcde",
      "messName": "Shree Krishna Bhojanalaya",
      "planId": "monthly",
      "planName": "Monthly Plan",
      "durationDays": 30,
      "price": 3200.0,
      "startDate": "2026-11-12",
      "endDate": "2026-12-12",
      "status": "ACTIVE",
      "createdAt": "2026-11-12T18:00:00Z",
      "updatedAt": "2026-11-12T18:00:00Z"
    }
  ],
  "timestamp": "2026-11-12T23:36:00"
}
```

#### Error Responses:
- `403 Forbidden`: Mess does not belong to the authenticated owner.
- `404 Not Found`: Mess not found.

---

### 2.6 Get All Platform Subscriptions (Admin)
- **Endpoint:** `GET /api/admin/subscriptions`
- **Authentication:** `Bearer <JWT_TOKEN>` (`ADMIN` role required)
- **Query Parameters:** `status` (optional: `ACTIVE`, `CANCELLED`, `EXPIRED`)
- **Success Code:** `200 OK`

#### Success Response (200 OK):
```json
{
  "success": true,
  "status": 200,
  "message": "All platform subscriptions fetched successfully",
  "data": [
    {
      "id": "6733a1b2c3d4e5f678901234",
      "userId": "67320a11f1234567890abcde",
      "userName": "Rohan Sharma",
      "userEmail": "rohan.sharma@example.com",
      "userPhone": "9876543210",
      "messId": "67320b98f1234567890abcde",
      "messName": "Shree Krishna Bhojanalaya",
      "planId": "monthly",
      "planName": "Monthly Plan",
      "durationDays": 30,
      "price": 3200.0,
      "startDate": "2026-11-12",
      "endDate": "2026-12-12",
      "status": "ACTIVE",
      "createdAt": "2026-11-12T18:00:00Z",
      "updatedAt": "2026-11-12T18:00:00Z"
    }
  ],
  "timestamp": "2026-11-12T23:37:00"
}
```

#### Error Responses:
- `403 Forbidden`: Non-admin caller attempting administrative inspection.

