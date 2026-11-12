# MessLoc – Module 5: Menu Management API Specification

## 1. Overview & Architectural Principles

Module 5 implements comprehensive, secure, and multi-tenant **Menu Management** for the MessLoc platform.

### Core Business Rules & Invariants:
1. **One Menu Per Mess Per Date**:
   A unique compound index is enforced at the database level on `{ "messId": 1, "menuDate": 1 }`. Attempting to create more than one menu for a mess on the same calendar date results in HTTP 409 Conflict.
2. **Owner-Only Menu Administration**:
   Only verified, active accounts with role `OWNER` who own the referenced mess facility can create, update, or delete menus and menu items. Attempting to manage another owner's menu results in HTTP 403 Forbidden.
3. **Item Uniqueness & Categorization**:
   Every item belongs to one of three `MealType` categories (`BREAKFAST`, `LUNCH`, `DINNER`). Food names must be unique within the same meal category for that day (case-insensitive); duplicate food names within the same meal category return HTTP 400 Bad Request.
4. **Public Discovery Visibility Gate**:
   Public menu viewing (`/api/menus/mess/{messId}/today` and `/api/menus/mess/{messId}/weekly`) is open to unauthenticated users, but **strictly requires** that the mess is verified (`verificationStatus == APPROVED`) and operational (`status == ACTIVE`). Any request for a pending, rejected, or inactive mess returns HTTP 404 Not Found to prevent enumeration or leaking unverified data.
5. **7-Day Weekly View Guarantee**:
   Both owner and public weekly views return seven consecutive calendar days starting from the requested or current date. Days with no recorded meals are gracefully represented with empty item arrays.

---

## 2. API Endpoints Reference

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

