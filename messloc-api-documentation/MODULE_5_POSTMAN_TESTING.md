# MessLoc – Module 5: Menu Management Postman Testing Guide

## 1. Prerequisites & Environment Setup

Before executing Menu Management requests:
1. Ensure the Spring Boot backend is running on `http://localhost:8080`.
2. Ensure you have an active `OWNER` JWT token obtained from `/api/auth/signin` after admin verification.
3. Set your Postman collection variables:
   - `baseUrl`: `http://localhost:8080`
   - `ownerToken`: Bearer JWT token of the active owner
   - `messId`: MongoDB ID of an APPROVED and ACTIVE mess owned by this owner
   - `menuId`: ID of the created daily menu (captured from Step 1)
   - `itemId`: ID of a menu item (captured from Step 1 or Step 3)

---

## 2. Recommended Testing Sequence

### Step 1: Create Daily Menu (Owner)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/menus`
- **Headers:** `Authorization: Bearer {{ownerToken}}`, `Content-Type: application/json`
- **Body:**
```json
{
  "messId": "{{messId}}",
  "menuDate": "2026-11-10",
  "items": [
    {
      "mealType": "BREAKFAST",
      "foodName": "Aloo Paratha",
      "description": "With fresh curd and butter"
    },
    {
      "mealType": "LUNCH",
      "foodName": "Dal Makhani",
      "description": "Slow-cooked black lentils"
    }
  ]
}
```
- **Expected Status:** `201 Created`
- **Action:** Save `data.id` as `{{menuId}}`.

---

### Step 2: Test Duplicate Menu Date Prevention
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/menus`
- **Headers:** `Authorization: Bearer {{ownerToken}}`, `Content-Type: application/json`
- **Body:** Same as Step 1.
- **Expected Status:** `409 Conflict`
- **Expected Message:** `A menu for this mess already exists on date 2026-11-10.`

---

### Step 3: Add Food Item to Existing Menu
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/menus/{{menuId}}/items`
- **Headers:** `Authorization: Bearer {{ownerToken}}`, `Content-Type: application/json`
- **Body:**
```json
{
  "mealType": "DINNER",
  "foodName": "Paneer Butter Masala",
  "description": "Rich tomato gravy"
}
```
- **Expected Status:** `201 Created`
- **Action:** Save `data.items[2].id` as `{{itemId}}`.

---

### Step 4: Update Food Item
- **Method:** `PUT`
- **URL:** `{{baseUrl}}/api/menus/{{menuId}}/items/{{itemId}}`
- **Headers:** `Authorization: Bearer {{ownerToken}}`, `Content-Type: application/json`
- **Body:**
```json
{
  "mealType": "DINNER",
  "foodName": "Shahi Paneer",
  "description": "Cashew and cream gravy"
}
```
- **Expected Status:** `200 OK`

---

### Step 5: Get Owner Weekly Menu View
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/menus/owner/mess/{{messId}}/weekly`
- **Headers:** `Authorization: Bearer {{ownerToken}}`
- **Expected Status:** `200 OK`
- **Verification:** Inspect `data.menus` array containing 7 consecutive days grouped into `breakfast`, `lunch`, `dinner`.

---

### Step 6: Get Public Today's Menu
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/menus/mess/{{messId}}/today`
- **Headers:** None (Unauthenticated)
- **Expected Status:** `200 OK`
- **Verification:** Verifies public visibility of today's menu.

---

### Step 7: Get Public Weekly Menu
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/menus/mess/{{messId}}/weekly`
- **Headers:** None (Unauthenticated)
- **Expected Status:** `200 OK`

---

### Step 8: Delete Food Item
- **Method:** `DELETE`
- **URL:** `{{baseUrl}}/api/menus/{{menuId}}/items/{{itemId}}`
- **Headers:** `Authorization: Bearer {{ownerToken}}`
- **Expected Status:** `200 OK`

---

### Step 9: Delete Daily Menu
- **Method:** `DELETE`
- **URL:** `{{baseUrl}}/api/menus/{{menuId}}`
- **Headers:** `Authorization: Bearer {{ownerToken}}`
- **Expected Status:** `200 OK`

