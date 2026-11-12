# MessLoc — Complete Backend REST API Reference & Specification

> **Application Name:** MessLoc (Mess Discovery & Management Platform)  
> **Backend Architecture:** Spring Boot 3.4.x / 4.x | Java 21 | Spring Security 6+ | JJWT 0.12.6 | Spring Data MongoDB  
> **Base Server URL:** `http://localhost:8080`  
> **Default Content-Type:** `application/json`  
> **Authentication Format:** `Authorization: Bearer <JWT_TOKEN>`  
> **Specification Version:** 1.0.0  
> **Status:** Production-Ready | 26 Implemented & Tested Endpoints  

---

## Table of Contents
1. [Architecture & Security Overview](#1-architecture--security-overview)
2. [Module 1: Authentication & Security](#module-1-authentication--security)
   - [1.1 Register Customer (USER)](#11-register-customer-user)
   - [1.2 Register Mess Owner (OWNER)](#12-register-mess-owner-owner)
   - [1.3 User / Owner / Admin Sign In](#13-user--owner--admin-sign-in)
   - [1.4 Get Current Authenticated Account Summary](#14-get-current-authenticated-account-summary)
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
8. [Standard Response & Error Formats](#standard-response--error-formats)

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
* **Purpose:** Retrieves all mess listings belonging to the authenticated owner.
* **HTTP Method:** `GET`
* **Endpoint URL:** `http://localhost:8080/api/owner/messes`
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

