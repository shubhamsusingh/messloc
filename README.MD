# MessLoc 🍱

**MessLoc** is a digital platform that helps students and working professionals discover and subscribe to nearby messes based on their food preferences, budget, meal type, and required facilities.

The project consists of an **Android mobile application** and a **Java Spring Boot backend** that provides REST APIs for authentication, mess management, subscriptions, and other platform functionality.

---

## 📌 Project Overview

Finding a reliable, affordable, and suitable mess can be difficult when moving to a new city.

MessLoc aims to simplify this process by allowing users to:

* 🔍 Discover nearby messes
* 🍛 Filter messes based on food and meal preferences
* 💰 Compare monthly prices
* 📍 Find messes based on location
* ⭐ View mess details and ratings
* 📦 Explore available meal packages
* 👤 Manage their profile and subscriptions
* 🏪 Allow mess owners to manage their mess information

The platform follows a **zero-commission model**, allowing users and mess owners to connect directly.

---

## 🏗️ Project Structure

```text
MessLoc/
│
├── android/                 # Android mobile application
│   ├── app/
│   ├── gradle/
│   ├── build.gradle
│   └── settings.gradle
│
├── backend/                 # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── ...
│
├── README.md
└── .gitignore
```

---

## 🛠️ Technology Stack

### Android

* Java
* Android SDK
* Android Studio
* XML
* Data Binding
* MVVM Architecture
* Retrofit
* Material Design

### Backend

* Java
* Spring Boot
* Spring Web
* Spring Data MongoDB
* Spring Security
* JWT Authentication
* Maven

### Database

* MongoDB

### Development Tools

* Git
* GitHub
* Postman
* Android Studio
* IntelliJ IDEA / Eclipse

---

## 🏛️ Architecture

```text
┌──────────────────────────┐
│      Android App         │
│                          │
│  UI → ViewModel → API    │
└────────────┬─────────────┘
             │
             │ HTTP / REST API
             ▼
┌──────────────────────────┐
│     Spring Boot API      │
│                          │
│ Controller               │
│      ↓                   │
│ Service                  │
│      ↓                   │
│ Repository               │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│        MongoDB           │
└──────────────────────────┘
```

---

## ✨ Key Features

### 👤 User

* User registration and login
* JWT-based authentication
* Browse nearby messes
* Search and filter messes
* View mess details
* View available packages
* Subscribe to a mess
* Manage profile

### 🏪 Mess Owner

* Owner registration/login
* Add and manage mess
* Update mess information
* Manage meal packages
* Manage pricing
* Manage facilities
* View subscriptions

### 🛡️ Authentication

The backend uses JWT-based authentication to secure protected APIs.

```text
Login
  ↓
Credentials Verification
  ↓
JWT Generation
  ↓
Authenticated API Requests
  ↓
Spring Security
```

---

## 📁 Backend Structure

The Spring Boot backend follows a layered architecture:

```text
backend/
└── src/
    └── main/
        ├── java/
        │   └── com/messloc/
        │       ├── controller/
        │       ├── service/
        │       ├── repository/
        │       ├── model/
        │       ├── dto/
        │       ├── security/
        │       └── config/
        │
        └── resources/
            └── application.properties
```

### Layers

**Controller**

Handles HTTP requests and responses.

**Service**

Contains application/business logic.

**Repository**

Handles database operations.

**Model**

Contains MongoDB entities/documents.

**DTO**

Defines request and response data structures.

**Security**

Handles authentication and authorization.

---

## 📱 Android Structure

The Android application follows an MVVM-oriented structure:

```text
android/
└── app/
    └── src/
        └── main/
            ├── java/
            │   └── com/example/messloc/
            │       ├── ui/
            │       ├── viewmodel/
            │       ├── model/
            │       ├── repository/
            │       └── network/
            │
            └── res/
                ├── layout/
                ├── drawable/
                ├── values/
                └── ...
```

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/MessLoc.git
cd MessLoc
```

---

# 📱 Android Setup

Open the `android` directory in **Android Studio**.

```text
MessLoc/
└── android/
```

Then:

1. Open the project in Android Studio.
2. Allow Gradle to sync.
3. Configure an Android emulator or physical device.
4. Configure the backend API URL.
5. Run the application.

---

# ☕ Spring Boot Setup

Navigate to the backend:

```bash
cd backend
```

Make sure Java and Maven are installed.

Check Java:

```bash
java -version
```

Check Maven:

```bash
mvn -version
```

Then configure your MongoDB connection and application settings.

Run the application:

```bash
mvn spring-boot:run
```

The backend will normally be available at:

```text
http://localhost:8080
```

---

## 🔐 Environment Variables

Do not commit passwords, API keys, JWT secrets, or database credentials to GitHub.

Example:

```env
MONGODB_URI=your_mongodb_connection_string
JWT_SECRET=your_jwt_secret
```

Keep sensitive configuration outside the public repository.

---

## 🔄 Development Workflow

Create a new branch before working on a feature:

```bash
git switch main
git pull origin main
git switch -c feature/your-feature
```

After completing your changes:

```bash
git add .
git commit -m "Add your feature"
git push -u origin feature/your-feature
```

Then create a **Pull Request** on GitHub.

### Branch Examples

```text
feature/android-login
feature/android-home
feature/spring-auth
feature/mess-api
feature/subscription
bugfix/login-validation
```

---

## 🧪 API Testing

Backend APIs can be tested using:

* Postman
* Android application
* Any REST API client

Example:

```text
POST /api/auth/register
POST /api/auth/login

GET    /api/messes
GET    /api/messes/{id}
POST   /api/messes
PUT    /api/messes/{id}
DELETE /api/messes/{id}
```

> API endpoints may change as development progresses.

---

## 🗺️ Future Improvements

* 🤖 AI-powered mess recommendations
* 📍 Location-based mess discovery
* 🗺️ Map integration
* ⭐ Reviews and ratings
* 🔔 Push notifications
* 💳 Online subscription/payment integration
* 📊 Mess owner analytics
* 📱 Improved Android UI/UX
* 🌐 Web-based administration dashboard

---

## 👥 Team

**Team Name:** Avengers

### Contributors

* Sri Om Sharan
* Team Members

---

## 📄 License

This project is developed for educational and project purposes.

---

## ⭐ Contributing

Contributions are welcome.

1. Fork the repository or get added as a collaborator.
2. Create a feature branch.
3. Make your changes.
4. Commit your changes.
5. Push the branch.
6. Create a Pull Request.
7. Wait for review and approval.

---

## 📞 Project

**MessLoc — Discover. Compare. Subscribe.**

A platform designed to make finding the right mess easier for students and working professionals.
