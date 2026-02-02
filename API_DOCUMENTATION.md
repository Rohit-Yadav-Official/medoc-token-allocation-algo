# OPD Token Allocation System - API Documentation

## Overview

This system provides a comprehensive token allocation engine for hospital OPD (Outpatient Department) that supports elastic capacity management, dynamic reallocation, and multiple booking sources.

## Base URL

```
http://localhost:9033/api
```

---

## API Endpoints

### Token Management

#### 1. Allocate Token
**POST** `/tokens/allocate`

Allocates a new token for a patient.

**Request Body:**
```json
{
  "patientId": "patient-uuid",
  "doctorId": "D1",
  "slot": "09-10",
  "visitDate": "2024-01-15",
  "bookingType": "ONLINE",
  "emergency": false
}
```

**Booking Types:**
- `ONLINE` - Online booking
- `WALK_IN` - Walk-in at OPD desk
- `PAID_PRIORITY` - Paid priority patients
- `FOLLOW_UP` - Follow-up patients

**Response:**
```json
{
  "tokenId": "token-uuid",
  "patientId": "patient-uuid",
  "doctorId": "D1",
  "slot": "09-10",
  "visitDate": "2024-01-15",
  "bookingType": "ONLINE",
  "status": "ALLOCATED",
  "priority": 4,
  "tokenNumber": 1,
  "emergency": false,
  "createdAt": "2024-01-15T08:00:00",
  "message": "Token allocated successfully"
}
```

**Status Codes:**
- `200 OK` - Token allocated successfully
- `400 Bad Request` - Invalid request (doctor/patient not found, slot unavailable)

---

#### 2. Get Token Details
**GET** `/tokens/{tokenId}`

Retrieves details of a specific token.

**Response:**
```json
{
  "tokenId": "token-uuid",
  "patientId": "patient-uuid",
  "doctorId": "D1",
  "slot": "09-10",
  "visitDate": "2024-01-15",
  "bookingType": "ONLINE",
  "status": "ALLOCATED",
  "priority": 4,
  "tokenNumber": 1,
  "emergency": false,
  "createdAt": "2024-01-15T08:00:00"
}
```

---

#### 3. Get Tokens by Slot
**GET** `/tokens/doctor/{doctorId}/date/{date}/slot/{slot}`

Retrieves all allocated tokens for a specific doctor-slot-date combination.

**Example:** `/tokens/doctor/D1/date/2024-01-15/slot/09-10`

**Response:** Array of Token objects

---

#### 4. Get Slot Capacity
**GET** `/tokens/slot-capacity/{doctorId}/{slot}/{date}`

Retrieves capacity information for a specific slot.

**Example:** `/tokens/slot-capacity/D1/09-10/2024-01-15`

**Response:**
```json
{
  "doctorId": "D1",
  "slot": "09-10",
  "visitDate": "2024-01-15",
  "maxCapacity": 10,
  "currentAllocated": 8,
  "availableSlots": 0
}
```

---

#### 5. Cancel Token
**PUT** `/tokens/{tokenId}/cancel?reason={reason}`

Cancels a token and triggers reallocation of waiting tokens.

**Query Parameters:**
- `reason` (optional) - Cancellation reason (default: "Patient cancellation")

**Response:**
```json
{
  "message": "Token cancelled successfully"
}
```

---

#### 6. Mark No-Show
**PUT** `/tokens/{tokenId}/no-show`

Marks a token as no-show (expired) and triggers reallocation.

**Response:**
```json
{
  "message": "Token marked as no-show"
}
```

---

#### 7. Insert Emergency Token
**PUT** `/tokens/{tokenId}/emergency`

Inserts an emergency token with highest priority.

**Response:**
```json
{
  "message": "Emergency token inserted successfully"
}
```

---

#### 8. Complete Token
**PUT** `/tokens/{tokenId}/complete`

Marks a token as completed and processes waiting queue.

**Response:**
```json
{
  "message": "Token marked as completed"
}
```

---

#### 9. Start Token (In-Progress)
**PUT** `/tokens/{tokenId}/start`

Marks a token as in-progress (currently being served).

**Response:**
```json
{
  "message": "Token marked as in-progress"
}
```

---

### Doctor Management

#### 10. Get All Doctors
**GET** `/doctors`

Retrieves all active doctors.

**Response:** Array of Doctor objects

---

#### 11. Get Doctor by ID
**GET** `/doctors/{doctorId}`

Retrieves a specific doctor's details.

**Response:** Doctor object

---

### Patient Management

#### 12. Get Patient
**GET** `/patients/{patientId}`

Retrieves a specific patient's details.

**Response:** Patient object

---

#### 13. Create Patient
**POST** `/patients`

Creates a new patient.

**Request Body:**
```json
{
  "name": "John Doe",
  "phoneNumber": "+1234567890",
  "email": "john.doe@example.com"
}
```

---

### Simulation

#### 14. Run OPD Day Simulation
**POST** `/simulation/run?date={date}`

Runs a complete simulation of one OPD day with multiple doctors, patients, and scenarios.

**Query Parameters:**
- `date` (optional) - Date for simulation (default: today)

**Response:**
```json
{
  "events": ["=== OPD Day Simulation ===", ...],
  "doctors": [...],
  "patients": [...],
  "allocatedTokens": [...],
  "cancellations": 3,
  "noShows": 2
}
```

---

## Token Status Flow

```
WAITING → ALLOCATED → IN_PROGRESS → COMPLETED
                ↓
            CANCELLED
                ↓
            EXPIRED (no-show)
```

---

## Priority System

Tokens are prioritized based on booking type and emergency status:

| Priority | Type | Description |
|----------|------|-------------|
| 0 | Emergency | Emergency cases (highest priority) |
| 1 | Paid Priority | Patients who paid for priority |
| 2 | Follow-up | Follow-up patients |
| 3 | Walk-in | Walk-in patients at OPD desk |
| 4 | Online | Online bookings (lowest priority) |

Within the same priority level, tokens are ordered by creation time (FIFO).

---

## Data Schema

### Token
- `tokenId` (UUID) - Unique token identifier
- `patientId` (UUID) - Patient identifier
- `doctorId` (String) - Doctor identifier (e.g., "D1")
- `slot` (String) - Time slot (e.g., "09-10")
- `visitDate` (LocalDate) - Visit date
- `bookingType` (Enum) - ONLINE, WALK_IN, PAID_PRIORITY, FOLLOW_UP
- `status` (Enum) - WAITING, ALLOCATED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED, REALLOCATED
- `priority` (int) - Priority level (0-4)
- `tokenNumber` (Integer) - Token number within slot
- `emergency` (boolean) - Emergency flag
- `createdAt` (LocalDateTime) - Creation timestamp
- `completedAt` (LocalDateTime) - Completion timestamp
- `expiredAt` (LocalDateTime) - Expiration timestamp

### Doctor
- `doctorId` (String) - Unique doctor identifier
- `name` (String) - Doctor name
- `speciality` (String) - Medical speciality
- `slots` (Set<String>) - Available time slots
- `workingDays` (Set<WorkingDay>) - Working days (MON, WED, FRI)
- `active` (boolean) - Active status

### Patient
- `id` (UUID) - Unique patient identifier
- `name` (String) - Patient name
- `phoneNumber` (String) - Phone number
- `email` (String) - Email address
- `status` (Enum) - ACTIVE, BLOCKED, DECEASED

