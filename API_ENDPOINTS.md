# Sportynix API Endpoints & Native Android Architecture Guide

This document provides a comprehensive guide to all REST API endpoints, request/response models, and usage details integrated into the **Sportynix** native Kotlin Android app (`/home/sam/Documents/Apps/Sportynix`).

---

## 1. What the Sportynix Kotlin Android App Is Built For

**Sportynix** is an enterprise-scale native Android application engineered in Kotlin using **Clean Architecture**, **MVVM**, **Jetpack Compose**, **Hilt**, **Retrofit**, **Room**, and **DataStore**.

Key application capabilities include:
- 🏟️ **Venue Discovery & Exploration**: Browse and search sports venues by category, location, and sport type.
- ⏰ **Slot Reservation & Booking**: Real-time slot availability lookup, temporary slot holding, and booking checkout.
- 🔐 **Authentication & Security**: Multi-step registration, 6-digit OTP verification, live username availability check, automatic JWT access token refresh (`TokenAuthenticator`), and encrypted DataStore token storage.
- 📱 **Push Notifications & Live Counter Sync**: Real-time unread messages & notification counts, OneSignal device registration.
- 💾 **Offline Caching & DB Fallback**: Room local database (`SportynixDatabase`) caching venues and user bookings for offline readability.

---

## 2. API Environments & Base URLs

Base URL is configured in `app/build.gradle.kts` (Active Default & Debug: `https://api.sportynix.com/`):

| Environment | Base URL | Status |
| :--- | :--- | :--- |
| **Live Server (Primary)** | `https://api.sportynix.com/` | **ACTIVE DEFAULT** (Debug, Release & Staging) |
| **Local Emulator** | `http://10.0.2.2:8000/` | Local Django dev server |

---

## 3. Endpoints & Usage Matrix

### 🔑 Authentication & Token Management

#### 1. Username Availability Check
- **Endpoint**: `GET /api/auth/username/check/?username={username}`
- **Header**: `No-Auth: true`
- **Usage**: Live debounced check during sign-up to verify if a username is available.
- **Response**:
```json
{
  "available": true,
  "reason": "available",
  "message": "Username is available"
}
```

#### 2. Sign In / Login
- **Endpoint**: `POST /api/auth/login/` (or `POST /api/token/`)
- **Header**: `No-Auth: true`
- **Request Body**:
```json
{
  "username_or_email": "alex.johnson@example.com",
  "password": "Password123"
}
```
- **Response**:
```json
{
  "access": "eyJhbGciOiJIUzI1Ni...",
  "refresh": "eyJhbGciOiJIUzI1Ni...",
  "user": {
    "id": "42",
    "username": "alexjohnson",
    "first_name": "Alex",
    "last_name": "Johnson",
    "email": "alex.johnson@example.com",
    "phone_number": "+1234567890",
    "profile_picture": null
  }
}
```

#### 3. Sign Up / Registration
- **Endpoint**: `POST /api/auth/signup/`
- **Header**: `No-Auth: true`
- **Request Body**:
```json
{
  "username": "alexjohnson",
  "first_name": "Alex",
  "last_name": "Johnson",
  "email": "alex.johnson@example.com",
  "phone_number": "+1234567890",
  "date_of_birth": "2000-01-01",
  "password": "Password123",
  "terms_accepted": true,
  "referral_code": "SPORTY2026"
}
```
- **Response**:
```json
{
  "session_id": "sess_8839201948",
  "message": "OTP verification code sent"
}
```

#### 4. Verify Sign-Up OTP
- **Endpoint**: `POST /api/auth/verify-signup/`
- **Header**: `No-Auth: true`
- **Request Body**:
```json
{
  "session_id": "sess_8839201948",
  "otp_code": "583920"
}
```
- **Response**: Returns JWT tokens (`access`, `refresh`) and `user` object.

#### 5. Resend OTP Code
- **Endpoint**: `POST /api/auth/resend-otp/`
- **Header**: `No-Auth: true`
- **Request Body**: `{ "session_id": "sess_8839201948" }`

#### 6. Synchronous Token Refresh
- **Endpoint**: `POST /api/token/refresh/`
- **Header**: `No-Auth: true`
- **Request Body**: `{ "refresh": "<refreshToken>" }`
- **Response**: `{ "access": "<newAccessToken>" }`

#### 7. Password Recovery & Reset
- **Forgot Password Request**: `POST /api/auth/forgot-password/` `{ "email": "user@example.com" }`
- **Reset Password Submit**: `POST /api/auth/reset-password/`
```json
{
  "email": "user@example.com",
  "otp_code": "123456",
  "new_password": "NewPassword123",
  "confirm_password": "NewPassword123"
}
```

---

### 🏟️ Venues & Time Slots

#### 1. Discover Venues
- **Endpoint**: `GET /api/venues/discover/?search={query}&venue_category={category}&page={page}&per_page={limit}&latitude={lat}&longitude={lon}`
- **Header**: `Authorization: Bearer <accessToken>`
- **Response**: Array of `VenueDto` items.

#### 2. Venue Detail
- **Endpoint**: `GET /api/venues/{id}/`
- **Header**: `Authorization: Bearer <accessToken>`

#### 3. Available Time Slots
- **Endpoint**: `GET /api/available_slots/{sportId}/?date={YYYY-MM-DD}&venue={venueId}`
- **Header**: `Authorization: Bearer <accessToken>`
- **Response**: Array of `TimeSlotDto` items.

---

### 📅 Bookings & QR Check-In

#### 1. Get User Bookings
- **Endpoint**: `GET /api/my-bookings/`
- **Header**: `Authorization: Bearer <accessToken>`

#### 2. Create Venue Booking
- **Endpoint**: `POST /api/bookings/`
- **Header**: `Authorization: Bearer <accessToken>`
- **Request Body**:
```json
{
  "venue_id": "venue_101",
  "slot_id": "slot_505",
  "booking_date": "2026-07-25"
}
```

#### 3. Get Booking QR Code
- **Endpoint**: `GET /api/my-bookings/{id}/qr_code/`
- **Header**: `Authorization: Bearer <accessToken>`

---

## 4. Kotlin Architecture Flow

```text
[Compose UI Screen]
       │ (Collects UiState / Triggers Event)
       ▼
  [ViewModel]
       │ (Executes Coroutine)
       ▼
   [UseCase]
       │ (Applies Validation & Business Rules)
       ▼
[Repository Interface]
       │
[Repository Implementation]
       ├───► [Retrofit ApiService] ──► [OkHttp AuthInterceptor & TokenAuthenticator] ──► [Backend API]
       └───► [Room Local Dao Database] (Offline Cache Storage)
```

---

## 5. Verification Command

To build a fresh Debug APK:
```bash
export JAVA_HOME=/opt/android-studio/jbr
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
```
Output APK location: [app-debug.apk](file:///home/sam/Documents/Apps/Sportynix/app/build/outputs/apk/debug/app-debug.apk)
