# MessLoc — Complete Backend REST API Reference & Specification

> **Application Name:** MessLoc (Mess Discovery & Management Platform)
> **Backend Architecture:** Spring Boot 3.4.x / 4.x | Java 21 | Spring Security 6+ | JJWT 0.12.6 | Spring Data MongoDB
> **Base Server URL:** `http://localhost:8080`
> **Default Content-Type:** `application/json`
> **Authentication Format:** `Authorization: Bearer <JWT_TOKEN>`
> **Specification Version:** 1.3.0
> **Status:** Production-Ready | 46 Production Endpoints + 2 Test Probes | 304/304 Automated Tests Passing

---

## Table of Contents
1. [Architecture & Security Overview](#1-architecture--security-overview)
2. [Module 1: Authentication & Security](#module-1-authentication--security)
   - [1.1 Register Customer (USER)](#11-register-customer-user)
   - [1.2 Register Mess Owner (OWNER)](#12-register-mess-owner-owner)
   - [1.3 User / Owner / Admin Sign In](#13-user--owner--admin-sign-in)
   - [1.4 Get Current Authenticated Account Summary](#14-get-current-authenticated-account-summary)
   - [1.5 Change Password](#15-change-password)
   - [1.6 Forgot Password](#16-forgot-password)
   - [1.7 Reset Password](#17-reset-password)
3. [Module 2: User Profile Management](#module-2-user-profile-management)
   - [2.1 Get My Profile](#21-get-my-profile)
   - [2.2 Update My Profile](#22-update-my-profile)
4. [Module 3: Mess Listing & Owner Management](#module-3-mess-listing--owner-management)
   - [3.1 Add Mess Listing](#31-add-mess-listing)
   - [3.2 Get My Mess Listings](#32-get-my-mess-listings)
   - [3.3 Get My Mess By ID](#33-get-my-mess-by-id)
   - [3.4 Update Mess Listing](#34-update-mess-listing)
   - [3.5 Delete Mess Listing](#35-delete-mess-listing)
   - [3.6 Activate Mess Listing](#36-activate-mess-listing)
   - [3.7 Deactivate Mess Listing](#37-deactivate-mess-listing)
5. [Module 4: Public Mess Discovery](#module-4-public-mess-discovery)
   - [4.1 Get All Approved Messes](#41-get-all-approved-messes)
   - [4.2 Filter Approved Messes by City](#42-filter-approved-messes-by-city)
   - [4.3 Get Public Mess Details by ID](#43-get-public-mess-details-by-id)
6. [Module 5: Admin Management & Workflows](#module-5-admin-management--workflows)
   - [5.1 List All Platform Messes](#51-list-all-platform-messes)
   - [5.2 List Pending Messes](#52-list-pending-messes)
   - [5.3 Approve Mess Listing](#53-approve-mess-listing)
   - [5.4 Reject Mess Listing](#54-reject-mess-listing)
   - [5.5 List Pending Owners](#55-list-pending-owners)
   - [5.6 Verify and Activate Owner](#56-verify-and-activate-owner)
   - [5.7 Block Owner Account](#57-block-owner-account)
   - [5.8 Create Administrator Account](#58-create-administrator-account)
   - [5.9 Admin Role Health Check](#59-admin-role-health-check)
7. [Module 6: Role Verification Test Endpoints](#module-6-role-verification-test-endpoints)
   - [6.1 Customer Role Access Test](#61-customer-role-access-test)
   - [6.2 Owner Role Access Test](#62-owner-role-access-test)
8. [Module 7: Menu Management](#module-7-menu-management)
   - [7.1 Create Daily Menu](#71-create-daily-menu)
   - [7.2 Update Daily Menu](#72-update-daily-menu)
   - [7.3 Add Menu Item](#73-add-menu-item)
   - [7.4 Update Menu Item](#74-update-menu-item)
   - [7.5 Delete Menu Item](#75-delete-menu-item)
   - [7.6 Delete Daily Menu](#76-delete-daily-menu)
   - [7.7 Get Owner Weekly Menu](#77-get-owner-weekly-menu)
   - [7.8 Get Public Today's Menu](#78-get-public-todays-menu)
   - [7.9 Get Public Weekly Menu](#79-get-public-weekly-menu)
9. [Module 8: Subscription Management](#module-8-subscription-management)
   - [8.1 Subscribe to Mess](#81-subscribe-to-mess)
   - [8.2 Get My Subscriptions](#82-get-my-subscriptions)
   - [8.3 Get Subscription Details by ID](#83-get-subscription-details-by-id)
   - [8.4 Cancel Active Subscription](#84-cancel-active-subscription)
   - [8.5 Get Subscriptions for Owner Mess](#85-get-subscriptions-for-owner-mess)
   - [8.6 List All Platform Subscriptions (Admin)](#86-list-all-platform-subscriptions-admin)
10. [Standard Response & Error Formats](#standard-response--error-formats)

---

## 1. Architecture & Security Overview

### 1.1 Envelope Structure (`ApiResponse<T>`)
Every endpoint (except authentication signin/signup which return specialized `AuthResponse`) wraps its response payload in a standard envelope:

```json
{
  "success": true,
  "status": 200,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

### 1.2 Authentication Lifecycle & Token Format
* **Algorithm:** HMAC-SHA512 (`HS512`) via JJWT `0.12.6`.
* **Claims:**
  * `sub`: Subject (normalized lowercase email).
  * `userId`: Unique MongoDB document identifier.
  * `role`: User role (`USER`, `OWNER`, `ADMIN`).
  * `iss`: Issuer (`MessLoc`).
  * `exp`: Expiration timestamp (24 hours = `86,400,000 ms`).
* **Header:** Send on all protected endpoints:
  ```http
  Authorization: Bearer <JWT_TOKEN>
  ```

### 1.3 Account Status Lifecycle
* `USER`: Automatically `ACTIVE` upon registration.
* `OWNER`: Initialized to `PENDING` upon registration. Cannot log in until approved by an `ADMIN`. Once verified, status becomes `ACTIVE`.
* `BLOCKED`: Account suspended by administrator. All authenticated requests return `403 Forbidden`.
* `INACTIVE`: Account deactivated.

---

## Module 1: Authentication & Security

### 1.1 Register Customer (USER)
* **Purpose:** Public registration endpoint for student or customer accounts.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/auth/signup/user`
* **Authentication:** None (Public)
* **Required Headers:** `Content-Type: application/json`
* **Validation Rules:**
  * `name`: Required, 2 to 50 characters.
  * `email`: Required, valid RFC email format, unique in database.
  * `password`: Required, minimum 8 characters (automatically BCrypt hashed with cost factor 12).
  * `phone`: Required, exactly 10 numeric digits (`^[0-9]{10}$`).
  * Role is server-enforced as `USER`; account status is server-enforced as `ACTIVE`.

#### Request Body
```json
{
  "name": "Aman Verma",
  "email": "aman.verma@example.com",
  "password": "Password@123",
  "phone": "9876543210"
}
```

#### Successful Response (`201 Created`)
```json
{
  "success": true,
  "message": "User registered successfully",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiVVNFUiIsInVzZXJJZCI6IjY3MmYwODc4MTEwYzE4MWU4ZWYyMDY4MyIsInN1YiI6ImFtYW4udmVybWFAZXhhbXBsZS5jb20iLCJpYXQiOjE3OTQyNDUwMDAsImV4cCI6MTc5NDMzMTQwMCwiaXNzIjoiTWVzc0xvYyJ9.xxxx",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": "672f0878110c181e8ef20683",
    "name": "Aman Verma",
    "email": "aman.verma@example.com",
    "phone": "9876543210",
    "city": null,
    "role": "USER",
    "status": "ACTIVE",
    "profileImage": null,
    "createdAt": "2026-11-09T19:50:00.123",
    "accountStatus": "ACTIVE"
  }
}
```

#### Error Responses
* **`400 Bad Request`** (Validation failure):
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Validation failed. Please correct the invalid fields.",
    "data": {
      "phone": "Phone number must be a valid 10-digit mobile number",
      "password": "Password must be at least 8 characters long"
    },
    "timestamp": "2026-11-09T19:50:00.123"
  }
  ```
* **`409 Conflict`** (Email already registered):
  ```json
  {
    "success": false,
    "status": 409,
    "message": "An account is already registered with email: aman.verma@example.com",
    "timestamp": "2026-11-09T19:50:00.123"
  }
  ```

---

### 1.2 Register Mess Owner (OWNER)
* **Purpose:** Public registration endpoint for mess business owners. Automatically initializes linked `OwnerProfile`.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/auth/signup/owner`
* **Authentication:** None (Public)
* **Required Headers:** `Content-Type: application/json`
* **Validation Rules:**
  * `name`: Required, 2–50 characters.
  * `email`: Required, valid email format, unique.
  * `password`: Required, minimum 8 characters.
  * `phone`: Required, exactly 10 digits.
  * `businessName`: Required, 3–100 characters.
  * `businessDescription`: Required, 10–500 characters.
  * `businessAddress`: Optional.
  * `fssaiLicenseNumber`: Optional.
  * Role is server-enforced as `OWNER`; account status is server-enforced as `PENDING`. No JWT token is issued until verified.

#### Request Body
```json
{
  "name": "Karan Joshi",
  "email": "karan.owner@example.com",
  "password": "OwnerPass@123",
  "phone": "9812345678",
  "businessName": "Karan Bhojnalaya & Tiffin Service",
  "businessDescription": "Authentic North Indian thali, wholesome tiffin packages, and hygienic meal service.",
  "businessAddress": "Plot 12, Clock Tower, Rajpur Road, Dehradun",
  "fssaiLicenseNumber": "12345678901234"
}
```

#### Successful Response (`201 Created`)
```json
{
  "success": true,
  "message": "Owner registered successfully. Your account is pending verification by an administrator.",
  "token": null,
  "tokenType": "Bearer",
  "expiresInMs": null,
  "user": {
    "id": "672f0999110c181e8ef20684",
    "name": "Karan Joshi",
    "email": "karan.owner@example.com",
    "phone": "9812345678",
    "city": null,
    "role": "OWNER",
    "status": "PENDING",
    "profileImage": null,
    "createdAt": "2026-11-09T19:50:00.123",
    "accountStatus": "PENDING"
  }
}
```

---

### 1.3 User / Owner / Admin Sign In
* **Purpose:** Authenticates existing account credentials and issues signed 24-hr JWT token.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/auth/signin`
* **Authentication:** None (Public)
* **Required Headers:** `Content-Type: application/json`

#### Request Body
```json
{
  "email": "aman.verma@example.com",
  "password": "Password@123"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiVVNFUiIsInVzZXJJZCI6IjY3MmYwODc4MTEwYzE4MWU4ZWYyMDY4MyIsInN1YiI6ImFtYW4udmVybWFAZXhhbXBsZS5jb20iLCJpYXQiOjE3OTQyNDUwMDAsImV4cCI6MTc5NDMzMTQwMCwiaXNzIjoiTWVzc0xvYyJ9.xxxx",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": "672f0878110c181e8ef20683",
    "name": "Aman Verma",
    "email": "aman.verma@example.com",
    "phone": "9876543210",
    "city": null,
    "role": "USER",
    "status": "ACTIVE",
    "profileImage": null,
    "createdAt": "2026-11-09T19:50:00.123",
    "accountStatus": "ACTIVE"
  }
}
```

#### Error Responses
* **`401 Unauthorized`** (Invalid password or non-existent email — generic message prevents email enumeration):
  ```json
  {
    "success": false,
    "status": 401,
    "message": "Invalid email or password",
    "timestamp": "2026-11-09T19:50:00.123"
  }
  ```
* **`403 Forbidden`** (Owner account awaiting verification):
  ```json
  {
    "success": false,
    "status": 403,
    "message": "Your account is pending admin approval. Please wait for verification.",
    "timestamp": "2026-11-09T19:50:00.123"
  }
  ```
* **`403 Forbidden`** (Account blocked by administrator):
  ```json
  {
    "success": false,
    "status": 403,
    "message": "Your account has been blocked. Please contact MessLoc support.",
    "timestamp": "2026-11-09T19:50:00.123"
  }
  ```

---

### 1.4 Get Current Authenticated Account Summary
* **Purpose:** Validates the active JWT token and returns authenticated identity.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/auth/me`
* **Authentication:** Required (Bearer Token)
* **Required Headers:** `Authorization: Bearer <TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "User retrieved successfully",
  "data": {
    "id": "672f0878110c181e8ef20683",
    "name": "Aman Verma",
    "email": "aman.verma@example.com",
    "phone": "9876543210",
    "city": null,
    "role": "USER",
    "status": "ACTIVE",
    "profileImage": null,
    "createdAt": "2026-11-09T19:50:00.123",
    "accountStatus": "ACTIVE"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 1.5 Change Password
* **Purpose:** Updates the authenticated account's password. Requires current password validation, enforces password complexity, prevents re-use of current password, and verifies confirmation match.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/auth/change-password`
* **Authentication:** Required (All roles: `USER`, `OWNER`, `ADMIN`)
* **Required Headers:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

#### Request Body
```json
{
  "currentPassword": "CurrentPassword@123",
  "newPassword": "NewStrongPassword@456",
  "confirmPassword": "NewStrongPassword@456"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Password changed successfully",
  "timestamp": "2026-11-14T19:00:00.000"
}
```

#### Error Responses
* **`400 Bad Request`** (Validation failure / password mismatch / password reuse / weak policy):
  ```json
  {
    "success": false,
    "status": 400,
    "message": "New password and confirmation password do not match",
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```
* **`401 Unauthorized`** (Incorrect current password or missing JWT):
  ```json
  {
    "success": false,
    "status": 401,
    "message": "Current password is incorrect",
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```

---

### 1.6 Forgot Password
* **Purpose:** Initiates a secure password reset workflow. Generates a cryptographically secure 32-byte token, computes its SHA-256 hash, stores the hash with a 15-minute TTL, invalidates prior unused tokens, and dispatches a reset link via email. Always returns a generic success message to prevent user enumeration. Rate limited to 3 requests per 15 minutes.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/auth/forgot-password`
* **Authentication:** None (Public Endpoint)
* **Required Headers:** `Content-Type: application/json`

#### Request Body
```json
{
  "email": "aman.verma@example.com"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "If the email is registered, password reset instructions have been sent.",
  "timestamp": "2026-11-14T19:00:00.000"
}
```

#### Error Responses
* **`400 Bad Request`** (Invalid email format):
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Validation failed. Please correct the invalid fields.",
    "data": {
      "email": "Email must be a valid email address"
    },
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```
* **`429 Too Many Requests`** (Rate limit exceeded: > 3 attempts within 15 minutes):
  ```json
  {
    "success": false,
    "status": 429,
    "message": "Too many password reset requests. Please wait a few minutes before trying again.",
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```

---

### 1.7 Reset Password
* **Purpose:** Completes the password reset process using a one-time reset token received via email. Validates token hash, checks expiry (15-minute window), enforces password complexity, encrypts password with BCrypt, marks token as used, and invalidates any outstanding tokens for the user.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/auth/reset-password`
* **Authentication:** None (Public Endpoint)
* **Required Headers:** `Content-Type: application/json`

#### Request Body
```json
{
  "token": "4a7b9c2d1e0f3a5b6c8d7e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b",
  "newPassword": "NewStrongPassword@456",
  "confirmPassword": "NewStrongPassword@456"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Password has been successfully reset. You can now log in with your new password.",
  "timestamp": "2026-11-14T19:00:00.000"
}
```

#### Error Responses
* **`400 Bad Request`** (Invalid, already used, or expired token):
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Invalid or already consumed password reset token",
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```
* **`400 Bad Request`** (Password mismatch or complexity violation):
  ```json
  {
    "success": false,
    "status": 400,
    "message": "New password and confirmation password do not match",
    "timestamp": "2026-11-14T19:00:00.000"
  }
  ```

---

## Module 2: User Profile Management

### 2.1 Get My Profile
* **Purpose:** Retrieves the authenticated user's profile details. Identity is derived strictly from `SecurityContext`.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/users/me`
* **Authentication:** Required (All roles: `USER`, `OWNER`, `ADMIN`)
* **Required Headers:** `Authorization: Bearer <TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "User profile retrieved successfully",
  "data": {
    "id": "672f0878110c181e8ef20683",
    "name": "Aman Verma",
    "email": "aman.verma@example.com",
    "phone": "9876543210",
    "city": "Dehradun",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "profileImage": "https://example.com/avatar.jpg",
    "createdAt": "2026-11-09T19:50:00.123"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 2.2 Update My Profile
* **Purpose:** Partially updates allowed profile fields (`name`, `phone`, `city`, `profileImage`). Protected fields (`id`, `email`, `password`, `role`, `status`) cannot be modified.
* **HTTP Method:** `PUT`
* **Endpoint URL:** `http://localhost:8080/api/users/me`
* **Authentication:** Required (All roles: `USER`, `OWNER`, `ADMIN`)
* **Required Headers:** `Content-Type: application/json`, `Authorization: Bearer <TOKEN>`
* **Validation Rules:**
  * `name`: Optional, 2–50 characters (cannot be blank if provided).
  * `phone`: Optional, 10 numeric digits.
  * `city`: Optional, 2–50 characters.
  * `profileImage`: Optional, max 255 characters.

#### Request Body
```json
{
  "name": "Aman V. Sharma",
  "phone": "9876500000",
  "city": "Dehradun",
  "profileImage": "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "User profile updated successfully",
  "data": {
    "id": "672f0878110c181e8ef20683",
    "name": "Aman V. Sharma",
    "email": "aman.verma@example.com",
    "phone": "9876500000",
    "city": "Dehradun",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "profileImage": "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80",
    "createdAt": "2026-11-09T19:50:00.123"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

## Module 3: Mess Listing & Owner Management

### 3.1 Add Mess Listing
* **Purpose:** Creates a new mess listing under the authenticated owner.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/messes` *(also supports alias `/api/owner/messes`)*
* **Authentication:** Required (Role: `OWNER`, Account Status: `ACTIVE`)
* **Required Headers:** `Content-Type: application/json`, `Authorization: Bearer <OWNER_TOKEN>`
* **Validation Rules:**
  * `name`: Required, 3–100 characters.
  * `description`: Required.
  * `address`, `city`, `state`: Required.
  * `pincode`: Required, exactly 6 digits (`^\\d{6}$`).
  * `latitude`: Required, decimal between `-90.0` and `90.0`.
  * `longitude`: Required, decimal between `-180.0` and `180.0`.
  * `contactNumber`: Required, valid 10-digit Indian mobile number (`^[6-9]\\d{9}$`).
  * `monthlyPrice`: Required, positive decimal > `0`.
  * `foodType`: Required enum (`VEG`, `NON_VEG`, `BOTH`).
  * `openingTime`: Required local time (`HH:mm` or `HH:mm:ss`). Must be strictly chronologically before `closingTime`.
  * `closingTime`: Required local time. Cannot equal or precede `openingTime`.
  * `facilities`: Optional list of strings.
  * `ownerId` is extracted automatically from `SecurityContext`.
  * `verificationStatus` is initialized to `PENDING`; operational status is initialized to `ACTIVE`.

#### Request Body
```json
{
  "name": "Annapurna Royal Mess",
  "description": "Premium pure veg North Indian meals with unlimited chapatis, salad, dessert on weekends, and hygienic RO water.",
  "address": "45/2 Subhash Nagar, Near Graphic Era University",
  "city": "Dehradun",
  "state": "Uttarakhand",
  "pincode": "248002",
  "latitude": 30.2687,
  "longitude": 77.9942,
  "contactNumber": "9876543210",
  "monthlyPrice": 3200.0,
  "foodType": "VEG",
  "openingTime": "07:30",
  "closingTime": "22:00",
  "facilities": [
    "RO Water",
    "High-Speed WiFi",
    "AC Dining Hall",
    "Dedicated Parking",
    "Weekend Special Sweets"
  ]
}
```

#### Successful Response (`201 Created`)
```json
{
  "success": true,
  "status": 201,
  "message": "Mess created successfully",
  "data": {
    "id": "672f0aaa110c181e8ef20685",
    "ownerId": "672f0999110c181e8ef20684",
    "name": "Annapurna Royal Mess",
    "description": "Premium pure veg North Indian meals with unlimited chapatis, salad, dessert on weekends, and hygienic RO water.",
    "address": "45/2 Subhash Nagar, Near Graphic Era University",
    "city": "Dehradun",
    "state": "Uttarakhand",
    "pincode": "248002",
    "latitude": 30.2687,
    "longitude": 77.9942,
    "contactNumber": "9876543210",
    "monthlyPrice": 3200.0,
    "foodType": "VEG",
    "openingTime": "07:30",
    "closingTime": "22:00",
    "facilities": [
      "RO Water",
      "High-Speed WiFi",
      "AC Dining Hall",
      "Dedicated Parking",
      "Weekend Special Sweets"
    ],
    "verificationStatus": "PENDING",
    "status": "ACTIVE",
    "createdAt": "2026-11-09T19:50:00.123Z",
    "updatedAt": "2026-11-09T19:50:00.123Z"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.2 Get My Mess Listings
* **Purpose:** Retrieves all mess listings belonging to the authenticated owner across all verification states (`PENDING`, `APPROVED`, `REJECTED`) and operational statuses (`ACTIVE`, `INACTIVE`). Supports optional `status` query filtering.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes`
* **Optional Query Parameters:**
  - `status` (String): Filter by verification status (`PENDING`, `APPROVED`, `REJECTED`) or operational status (`ACTIVE`, `INACTIVE`).
    - `GET /api/owner/messes?status=PENDING`
    - `GET /api/owner/messes?status=APPROVED`
    - `GET /api/owner/messes?status=REJECTED`
    - `GET /api/owner/messes?status=ACTIVE`
    - `GET /api/owner/messes?status=INACTIVE`
* **Authentication:** Required (Role: `OWNER`)
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Messes fetched successfully",
  "data": [
    {
      "id": "672f0aaa110c181e8ef20685",
      "ownerId": "672f0999110c181e8ef20684",
      "name": "Annapurna Royal Mess",
      "city": "Dehradun",
      "monthlyPrice": 3200.0,
      "foodType": "VEG",
      "verificationStatus": "PENDING",
      "status": "ACTIVE"
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.3 Get My Mess By ID
* **Purpose:** Retrieves details of a specific mess. Enforces ownership authorization.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes/{id}`
* **Authentication:** Required (Role: `OWNER`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess fetched successfully",
  "data": {
    "id": "672f0aaa110c181e8ef20685",
    "ownerId": "672f0999110c181e8ef20684",
    "name": "Annapurna Royal Mess",
    "description": "Premium pure veg North Indian meals...",
    "city": "Dehradun",
    "monthlyPrice": 3200.0,
    "foodType": "VEG",
    "verificationStatus": "PENDING",
    "status": "ACTIVE"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

#### Error Response (`403 Forbidden` — IDOR Protection)
```json
{
  "success": false,
  "status": 403,
  "message": "You are not authorized to manage this mess",
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.4 Update Mess Listing
* **Purpose:** Updates existing mess details. Enforces ownership authorization.
* **HTTP Method:** `PUT`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes/{id}` *(alias `/api/messes/{id}`)*
* **Authentication:** Required (Role: `OWNER`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Content-Type: application/json`, `Authorization: Bearer <OWNER_TOKEN>`

#### Request Body
```json
{
  "name": "Annapurna Royal Mess & Caterers",
  "description": "Newly renovated premium veg & non-veg meals with special Sunday desserts and unlimited salad bar.",
  "address": "45/2 Subhash Nagar, Opposite GEU Gate 2",
  "city": "Dehradun",
  "state": "Uttarakhand",
  "pincode": "248002",
  "latitude": 30.2687,
  "longitude": 77.9942,
  "contactNumber": "9876543210",
  "monthlyPrice": 3500.0,
  "foodType": "BOTH",
  "openingTime": "07:00",
  "closingTime": "22:30",
  "facilities": [
    "RO Water",
    "High-Speed WiFi",
    "AC Dining Hall",
    "Two-Wheeler Parking",
    "CCTV Surveillance"
  ],
  "status": "ACTIVE"
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess updated successfully",
  "data": {
    "id": "672f0aaa110c181e8ef20685",
    "ownerId": "672f0999110c181e8ef20684",
    "name": "Annapurna Royal Mess & Caterers",
    "monthlyPrice": 3500.0,
    "foodType": "BOTH",
    "verificationStatus": "PENDING",
    "status": "ACTIVE"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.5 Delete Mess Listing
* **Purpose:** Deletes a mess listing permanently. Enforces ownership authorization.
* **HTTP Method:** `DELETE`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes/{id}` *(alias `/api/messes/{id}`)*
* **Authentication:** Required (Role: `OWNER`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess deleted successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.6 Activate Mess Listing
* **Purpose:** Sets mess operational status to `ACTIVE`.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes/{id}/activate` *(alias `/api/messes/{id}/activate`)*
* **Authentication:** Required (Role: `OWNER`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess activated successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 3.7 Deactivate Mess Listing
* **Purpose:** Sets mess operational status to `INACTIVE`. Removes the listing from public discovery without deleting it.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes/{id}/deactivate` *(alias `/api/messes/{id}/deactivate`)*
* **Authentication:** Required (Role: `OWNER`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess deactivated successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

## Module 4: Public Mess Discovery

### 4.1 Get All Approved Messes
* **Purpose:** Returns list of all messes that have `verificationStatus == APPROVED` and `status == ACTIVE`. Omits unapproved or inactive listings and hides sensitive owner data.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/messes`
* **Authentication:** None (Public)

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Messes fetched successfully",
  "data": [
    {
      "id": "672f0aaa110c181e8ef20685",
      "name": "Annapurna Royal Mess",
      "description": "Premium pure veg North Indian meals...",
      "address": "45/2 Subhash Nagar, Near Graphic Era University",
      "city": "Dehradun",
      "state": "Uttarakhand",
      "pincode": "248002",
      "latitude": 30.2687,
      "longitude": 77.9942,
      "contactNumber": "9876543210",
      "monthlyPrice": 3200.0,
      "foodType": "VEG",
      "openingTime": "07:30",
      "closingTime": "22:00",
      "facilities": ["RO Water", "WiFi", "AC Dining Hall"]
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 4.2 Filter Approved Messes by City
* **Purpose:** Public search with case-insensitive city matching.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/messes?city=Dehradun`
* **Authentication:** None (Public)
* **Query Parameters:**
  * `city` (String, optional) — Filter city name (e.g. `Dehradun`, `Delhi`, `Pune`).

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Messes fetched successfully",
  "data": [
    {
      "id": "672f0aaa110c181e8ef20685",
      "name": "Annapurna Royal Mess",
      "city": "Dehradun",
      "monthlyPrice": 3200.0,
      "foodType": "VEG"
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 4.3 Get Public Mess Details by ID
* **Purpose:** Retrieves public details of a specific approved mess.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/messes/{id}`
* **Authentication:** None (Public)
* **Path Parameters:** `id` — Unique mess identifier.

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess fetched successfully",
  "data": {
    "id": "672f0aaa110c181e8ef20685",
    "name": "Annapurna Royal Mess",
    "description": "Premium pure veg North Indian meals...",
    "address": "45/2 Subhash Nagar, Near Graphic Era University",
    "city": "Dehradun",
    "state": "Uttarakhand",
    "pincode": "248002",
    "latitude": 30.2687,
    "longitude": 77.9942,
    "contactNumber": "9876543210",
    "monthlyPrice": 3200.0,
    "foodType": "VEG",
    "openingTime": "07:30",
    "closingTime": "22:00",
    "facilities": ["RO Water", "WiFi", "AC Dining Hall"]
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

#### Error Response (`404 Not Found` — Unapproved or Inactive)
```json
{
  "success": false,
  "status": 404,
  "message": "Mess is not publicly available or pending verification",
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

## Module 5: Admin Management & Workflows

### 5.1 List All Platform Messes
* **Purpose:** Retrieves all mess listings in the platform across all verification and operational statuses.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/admin/messes`
* **Authentication:** Required (Role: `ADMIN`)
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "All messes fetched successfully",
  "data": [
    {
      "id": "672f0aaa110c181e8ef20685",
      "ownerId": "672f0999110c181e8ef20684",
      "name": "Annapurna Royal Mess",
      "verificationStatus": "PENDING",
      "status": "ACTIVE"
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.2 List Pending Messes
* **Purpose:** Lists all mess listings awaiting administrator review.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/admin/messes/pending`
* **Authentication:** Required (Role: `ADMIN`)
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Pending messes fetched successfully",
  "data": [
    {
      "id": "672f0aaa110c181e8ef20685",
      "name": "Annapurna Royal Mess",
      "verificationStatus": "PENDING"
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.3 Approve Mess Listing
* **Purpose:** Sets `verificationStatus = APPROVED`, immediately exposing the mess to public discovery.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/admin/messes/{id}/approve`
* **Authentication:** Required (Role: `ADMIN`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess approved successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.4 Reject Mess Listing
* **Purpose:** Sets `verificationStatus = REJECTED`. Prevents the listing from appearing in public discovery.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/admin/messes/{id}/reject`
* **Authentication:** Required (Role: `ADMIN`)
* **Path Parameters:** `id` — Unique mess identifier.
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Mess rejected successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.5 List Pending Owners
* **Purpose:** Lists all mess owner accounts awaiting admin verification.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/admin/owners/pending`
* **Authentication:** Required (Role: `ADMIN`)
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Pending owners fetched successfully",
  "data": [
    {
      "id": "672f0999110c181e8ef20684",
      "name": "Karan Joshi",
      "email": "karan.owner@example.com",
      "phone": "9812345678",
      "city": null,
      "role": "OWNER",
      "status": "PENDING",
      "createdAt": "2026-11-09T19:50:00.123"
    }
  ],
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.6 Verify and Activate Owner
* **Purpose:** Activates the owner account (`status = ACTIVE`) and verifies their linked profile (`isVerified = true`), granting them access to sign in and publish mess listings.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/admin/owners/{userId}/verify`
* **Authentication:** Required (Role: `ADMIN`)
* **Path Parameters:** `userId` — Unique user ID of the owner.
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Owner verified and activated successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.7 Block Owner Account
* **Purpose:** Suspends an owner account (`status = BLOCKED`), immediately revoking their signin ability and API permissions.
* **HTTP Method:** `PATCH`
* **Endpoint URL:** `http://localhost:8080/api/admin/owners/{userId}/block`
* **Authentication:** Required (Role: `ADMIN`)
* **Path Parameters:** `userId` — Unique user ID of the owner.
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Owner account blocked successfully",
  "data": null,
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.8 Create Administrator Account
* **Purpose:** Creates an additional administrator account. Accessible strictly to existing authenticated, active administrators.
* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/admin/create-admin` *(alias `/api/admin/create`)*
* **Authentication:** Required (Role: `ADMIN`, Status: `ACTIVE`)
* **Required Headers:** `Content-Type: application/json`, `Authorization: Bearer <ADMIN_TOKEN>`

#### Request Body
```json
{
  "name": "Operations Admin",
  "email": "ops.admin@messloc.com",
  "password": "Admin@12345",
  "phone": "9876543299"
}
```

#### Successful Response (`201 Created`)
```json
{
  "success": true,
  "status": 201,
  "message": "New admin account created successfully",
  "data": {
    "id": "672f0bbb110c181e8ef20686",
    "name": "Operations Admin",
    "email": "ops.admin@messloc.com",
    "phone": "9876543299",
    "city": null,
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2026-11-09T19:50:00.123"
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 5.9 Admin Role Health Check
* **Purpose:** Verifies that the caller's JWT token holds the `ROLE_ADMIN` authority.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/admin/test`
* **Authentication:** Required (Role: `ADMIN`)
* **Required Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Access granted to ADMIN endpoint",
  "data": {
    "role": "ADMIN",
    "username": "admin@messloc.com",
    "message": "Welcome Admin! You have successfully verified access to protected administrative controls."
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

## Module 6: Role Verification Test Endpoints

### 6.1 Customer Role Access Test
* **Purpose:** Verifies that caller holds `ROLE_USER`.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/user/test`
* **Authentication:** Required (Role: `USER`)
* **Required Headers:** `Authorization: Bearer <USER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Access granted to USER endpoint",
  "data": {
    "role": "USER",
    "username": "aman.verma@example.com",
    "message": "Welcome User! You have successfully verified access to protected customer features."
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

### 6.2 Owner Role Access Test
* **Purpose:** Verifies that caller holds `ROLE_OWNER`.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/owner/test`
* **Authentication:** Required (Role: `OWNER`)
* **Required Headers:** `Authorization: Bearer <OWNER_TOKEN>`

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Access granted to OWNER endpoint",
  "data": {
    "role": "OWNER",
    "username": "karan.owner@example.com",
    "message": "Welcome Owner! You have successfully verified access to protected business owner features."
  },
  "timestamp": "2026-11-09T19:50:00.123"
}
```

---

## Module 7: Menu Management

### Core Architectural Invariants
1. One menu per mess per calendar date (enforced by unique MongoDB index `{messId: 1, menuDate: 1}`).
2. Only verified, active owners who own the mess can create, update, or delete menus and items.
3. Item uniqueness by meal category (`BREAKFAST`, `LUNCH`, `DINNER`).
4. Public discovery gate: public viewing requires mess `verificationStatus == APPROVED` and `status == ACTIVE`.
5. 7-day weekly menu guarantee with empty arrays for unrecorded days.

### 2.1 Create Daily Menu
- **Endpoint:** `POST /api/menus`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `201 Created`
- **Response Type:** `ApiResponse<MenuResponse>`

#### Request Body:
```json
{
  "messId": "675abc123456789012345678",
  "menuDate": "2026-11-10",
  "items": [
    {
      "mealType": "BREAKFAST",
      "foodName": "Aloo Paratha",
      "description": "Stuffed potato flatbread served with fresh curd and pickle"
    },
    {
      "mealType": "LUNCH",
      "foodName": "Dal Tadka",
      "description": "Yellow lentils tempered with garlic and cumin"
    },
    {
      "mealType": "DINNER",
      "foodName": "Paneer Butter Masala",
      "description": "Cottage cheese cubes in rich tomato gravy"
    }
  ]
}
```

#### Success Response (201 Created):
```json
{
  "success": true,
  "status": 201,
  "message": "Daily menu created successfully",
  "data": {
    "id": "675menu12345678901234567",
    "messId": "675abc123456789012345678",
    "menuDate": "2026-11-10",
    "items": [
      {
        "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d001",
        "mealType": "BREAKFAST",
        "foodName": "Aloo Paratha",
        "description": "Stuffed potato flatbread served with fresh curd and pickle"
      },
      {
        "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d002",
        "mealType": "LUNCH",
        "foodName": "Dal Tadka",
        "description": "Yellow lentils tempered with garlic and cumin"
      },
      {
        "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d003",
        "mealType": "DINNER",
        "foodName": "Paneer Butter Masala",
        "description": "Cottage cheese cubes in rich tomato gravy"
      }
    ],
    "createdAt": "2026-11-10T12:00:00Z",
    "updatedAt": "2026-11-10T12:00:00Z"
  }
}
```

---

### 2.2 Update Daily Menu
- **Endpoint:** `PUT /api/menus/{menuId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<MenuResponse>`

#### Request Body:
```json
{
  "items": [
    {
      "mealType": "BREAKFAST",
      "foodName": "Poha & Tea",
      "description": "Flattened rice tempered with mustard seeds and curry leaves"
    },
    {
      "mealType": "LUNCH",
      "foodName": "Rajma Chawal",
      "description": "Kidney bean curry with steamed basmati rice"
    }
  ]
}
```

---

### 2.3 Add Food Item to Menu
- **Endpoint:** `POST /api/menus/{menuId}/items`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `201 Created`
- **Response Type:** `ApiResponse<MenuResponse>`

#### Request Body:
```json
{
  "mealType": "DINNER",
  "foodName": "Mix Veg Curry",
  "description": "Seasonal vegetables cooked in traditional spices"
}
```

---

### 2.4 Update Food Item
- **Endpoint:** `PUT /api/menus/{menuId}/items/{itemId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<MenuResponse>`

#### Request Body:
```json
{
  "mealType": "DINNER",
  "foodName": "Shahi Paneer",
  "description": "Royal cottage cheese in cashew cream sauce"
}
```

---

### 2.5 Delete Food Item
- **Endpoint:** `DELETE /api/menus/{menuId}/items/{itemId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<MenuResponse>`

---

### 2.6 Delete Daily Menu
- **Endpoint:** `DELETE /api/menus/{menuId}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<Void>`

---

### 2.7 Get Owner Weekly Menu View
- **Endpoint:** `GET /api/menus/owner/mess/{messId}/weekly?startDate={yyyy-MM-dd}`
- **Authentication:** `Bearer <JWT_TOKEN>` (`OWNER` only)
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<WeeklyMenuResponse>`

#### Success Response:
```json
{
  "success": true,
  "status": 200,
  "message": "Weekly menu fetched successfully",
  "data": {
    "messId": "675abc123456789012345678",
    "messName": "Annapurna Mess",
    "startDate": "2026-11-10",
    "endDate": "2026-11-16",
    "menus": [
      {
        "menuId": "675menu12345678901234567",
        "messId": "675abc123456789012345678",
        "menuDate": "2026-11-10",
        "breakfast": [
          {
            "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d001",
            "mealType": "BREAKFAST",
            "foodName": "Aloo Paratha",
            "description": "Stuffed potato flatbread"
          }
        ],
        "lunch": [
          {
            "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d002",
            "mealType": "LUNCH",
            "foodName": "Dal Tadka",
            "description": "Yellow lentils tempered with spices"
          }
        ],
        "dinner": [
          {
            "id": "c1f72b9a-4c2e-4b68-bfa9-1e3c88a8d003",
            "mealType": "DINNER",
            "foodName": "Shahi Paneer",
            "description": "Royal cottage cheese in cashew cream"
          }
        ]
      }
    ]
  }
}
```

---

### 2.8 Get Public Today's Menu
- **Endpoint:** `GET /api/menus/mess/{messId}/today`
- **Authentication:** `Public (No token required)`
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<DailyMenuResponse>`

---

### 2.9 Get Public Weekly Menu View
- **Endpoint:** `GET /api/menus/mess/{messId}/weekly?startDate={yyyy-MM-dd}`
- **Authentication:** `Public (No token required)`
- **Success Code:** `200 OK`
- **Response Type:** `ApiResponse<WeeklyMenuResponse>`


---

## Module 8: Subscription Management

### Core Architectural Invariants
1. Only authenticated customers (`Role.USER`, `AccountStatus.ACTIVE`) can subscribe or cancel.
2. Mess eligibility: target mess must be `VerificationStatus.APPROVED` and `MessStatus.ACTIVE`.
3. Concurrency & duplicate prevention: maximum of 1 `ACTIVE` subscription per user per mess.
4. Backend-derived dates: `startDate` defaults to today; `endDate` is calculated from duration.
5. Non-destructive cancellation: subscription status updated to `CANCELLED`.
6. Multi-tenant IDOR protection: customers view only their own subscriptions; owners view only subscriptions for their own messes; administrators have global oversight.

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


---

## Standard Response & Error Formats

### Error Format Reference Table

| Status Code | Meaning | Trigger Condition | Sample JSON Response |
|---|---|---|---|
| **400** | Bad Request | Validation failure on request DTO or invalid time order | `{"success":false,"status":400,"message":"Validation failed. Please correct the invalid fields.","data":{"phone":"..."}}` |
| **401** | Unauthorized | Missing JWT, expired token, or invalid signin password | `{"success":false,"status":401,"message":"Invalid email or password"}` |
| **403** | Forbidden | Insufficient role, pending owner signin, or cross-tenant IDOR | `{"success":false,"status":403,"message":"Access denied: You do not have permission to access this resource"}` |
| **404** | Not Found | Mess ID or User ID not found in database | `{"success":false,"status":404,"message":"Mess not found with id: 123"}` |
| **409** | Conflict | Duplicate email address registration attempt | `{"success":false,"status":409,"message":"An account is already registered with email: ..."}` |
| **500** | Server Error | Unhandled internal exception (generic safe message) | `{"success":false,"status":500,"message":"An unexpected internal server error occurred."}` |

