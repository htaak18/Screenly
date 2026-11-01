# REPORT OUTLINE
## Online Movie Ticket Booking System
### Project-Based Learning (PBL) – 5th Semester | Department of Computer Engineering

---

> **Instructions:** Replace all `[bracketed placeholders]` with your actual content.  
> Target length: 40–60 pages including diagrams and code snippets.

---

## Cover Page
- Title: **Online Movie Ticket Booking System**
- Subject: [Subject Name & Code]
- Submitted by: [Your Name] | Roll No: [XXXX] | Division: [X]
- Guide: [Professor's Name]
- Institute: [College Name]
- Year: 2025–26

---

## Declaration
*"We hereby declare that this project report is our own original work..."*

---

## Certificate
*(Guide's sign-off confirming the project is submitted in partial fulfilment of...)*

---

## Acknowledgements
Thank your guide, Head of Department, and any peers who helped.

---

## Table of Contents
*(Auto-generated from section headings)*

---

## List of Figures
| Figure | Title | Page |
|---|---|---|
| 1 | System Architecture Diagram | X |
| 2 | Seat State Machine Diagram | X |
| 3 | Class Diagram | X |
| 4 | Sequence Diagram – Seat Hold Flow | X |
| 5 | ER Diagram of XML Schema | X |
| 6 | Screenshot – Login Page | X |
| 7 | Screenshot – Movie List | X |
| 8 | Screenshot – Seat Selection | X |
| 9 | Screenshot – Payment Page | X |
| 10 | Screenshot – Booking Confirmation | X |
| 11 | Test Results – 50-Thread Concurrent Hold | X |

---

## Abstract
*(~200 words)*

This report presents the design and implementation of an **Online Movie Ticket Booking System** built as a university Project-Based Learning submission. The system allows users to browse movies, view show timings, interactively select seats, make a mock payment, and manage their bookings. An administrator role enables adding movies and shows.

The project uses Java 17, Jakarta Servlets, JSP with JSTL, and XML file storage — deliberately avoiding frameworks and databases to emphasise core Java web programming concepts.

The central challenge addressed is **concurrent seat booking**: multiple users simultaneously trying to hold the same seat. The solution employs a `ConcurrentHashMap` of `ReentrantLock` objects (one per show), ensuring that the entire check-then-hold sequence is atomic per show while allowing independent shows to proceed in parallel. A dual-expiry strategy (lazy expiry on every read + a `ScheduledExecutorService` running every 10 seconds) ensures held seats are correctly released after 7 minutes. Atomic XML writes via temp-file-then-rename prevent data corruption.

All concurrency claims are verified by seven JUnit 5 tests, including a 50-thread race test and a side-by-side demonstration of an unsafe (un-locked) implementation versus the safe implementation.

---

## 1. Introduction

### 1.1 Background
Online ticket booking systems have transformed how audiences access entertainment. Systems like BookMyShow handle millions of concurrent users racing to book seats for popular shows. The primary technical challenge is preventing two users from booking the same seat simultaneously — a classic concurrency problem.

### 1.2 Problem Statement
Design and implement an online movie ticket booking system that:
- Allows multiple users to concurrently browse and book seats
- Guarantees no two users can book the same seat
- Provides a 7-minute hold-then-pay flow with server-enforced expiry
- Uses XML for data storage (no database)
- Is deployable on Apache Tomcat 10.1

### 1.3 Scope
- User registration and login
- Movie/show management (admin)
- Interactive seat selection with live updates
- Hold → Pay → Confirm flow
- Booking cancellation
- Comprehensive concurrency testing

---

## 2. Objectives

1. Implement a complete web application using Java Servlets and JSP
2. Store all data in XML files using the DOM parser (no database)
3. Prevent concurrent double-booking using `ReentrantLock` per show
4. Implement and demonstrate hold expiry using injectable `java.time.Clock`
5. Write 7 JUnit 5 tests proving concurrency correctness
6. Secure the application: hashed passwords (SHA-256 + salt), CSRF tokens, XSS prevention
7. Deploy on Tomcat 10.1 using `mvn cargo:run`

---

## 3. Requirement Analysis

### 3.1 Functional Requirements

| FR# | Requirement |
|---|---|
| FR1 | Users can register with username, email, and password |
| FR2 | Users can log in and log out (session-based) |
| FR3 | Users can browse all movies |
| FR4 | Users can view show timings for a movie |
| FR5 | Users can select seats on a 5×8 grid (max 6 seats) |
| FR6 | System holds selected seats for 7 minutes on "Hold Seats" |
| FR7 | User completes mock payment within hold window |
| FR8 | Successful payment creates a CONFIRMED booking |
| FR9 | Payment failure releases seats back to AVAILABLE |
| FR10 | Users can view and cancel their future bookings |
| FR11 | Admin can add movies and shows |
| FR12 | Seat grid auto-refreshes every 5 seconds |

### 3.2 Non-Functional Requirements

| NFR# | Requirement |
|---|---|
| NFR1 | Zero double-bookings under concurrent load |
| NFR2 | Hold expiry enforced server-side (not browser) |
| NFR3 | XML writes are crash-safe (atomic rename) |
| NFR4 | Session cookie is HttpOnly (XSS protection) |
| NFR5 | All user-controlled output escaped (JSTL `c:out`) |
| NFR6 | Mutating forms protected with CSRF tokens |
| NFR7 | Responsive UI (works on mobile/tablet) |

---

## 4. Technology Stack

| Component | Technology | Reason |
|---|---|---|
| Language | Java 17 | LTS version; modern features (Records, switch expressions) |
| Web Layer | Jakarta Servlets 6.0 | Tomcat 10.1 requirement; jakarta.* not javax.* |
| View Layer | JSP + JSTL 3.0 | Standard university curriculum; c:out prevents XSS |
| Data Storage | XML + DOM Parser | Project requirement; demonstrates file I/O + XML parsing |
| Build Tool | Apache Maven 3.x | Dependency management; WAR packaging; Cargo plugin |
| Server | Tomcat 10.1 (embedded via Cargo) | Simplest deployment; `mvn cargo:run` |
| Testing | JUnit 5.10 | Standard Java testing framework; parameterised tests |
| Security | SHA-256 + salt | Password hashing; CSRF tokens; HttpOnly cookies |

---

## 5. System Design

### 5.1 Architecture Diagram
*(Draw or describe a layered architecture diagram)*

```
┌─────────────────────────────────────────────────────┐
│                   Client (Browser)                  │
│          HTML/CSS/JS  ←→  HTTP Requests             │
└─────────────────────────────┬───────────────────────┘
                              │ Jakarta Servlet API
┌─────────────────────────────▼───────────────────────┐
│                  Tomcat 10.1 Container               │
│  ┌──────────┐  ┌───────────┐  ┌──────────────────┐  │
│  │AuthFilter│  │AdminFilter│  │AppContextListener│  │
│  └────┬─────┘  └─────┬─────┘  └────────┬─────────┘  │
│       │              │                  │             │
│  ┌────▼──────────────▼─────────────┐   │             │
│  │         Servlet Layer (15)      │   │             │
│  └────────────────┬────────────────┘   │             │
│                   │                    │             │
│  ┌────────────────▼────────────────┐   │             │
│  │     Service Layer               │   │             │
│  │  SeatService  |  AuthService    │◄──┘             │
│  └────────────────┬────────────────┘                 │
│                   │                                  │
│  ┌────────────────▼────────────────┐                 │
│  │     Store Layer (XML I/O)       │                 │
│  │  XmlStore + 5 entity Stores     │                 │
│  └────────────────┬────────────────┘                 │
└───────────────────┼──────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────┐
│           File System (XML Data Files)               │
│  users.xml  movies.xml  shows.xml  seats.xml         │
│  bookings.xml                                        │
└──────────────────────────────────────────────────────┘
```

### 5.2 Class Diagram
*(Describe or draw UML Class Diagram)*

**Model classes:** `User`, `Movie`, `Show`, `Seat` (with inner `SeatState` enum: AVAILABLE/HELD/BOOKED), `Booking` (with inner `BookingStatus` enum: CONFIRMED/CANCELLED)

**Service classes:** `SeatService` (depends on `SeatStore`, `BookingStore`, `Clock`), `AuthService` (depends on `UserStore`)

**Store classes:** `XmlStore` (static helpers), `UserStore`, `MovieStore`, `ShowStore`, `SeatStore`, `BookingStore` (each has a `ReadWriteLock` field and a `Path` field)

**Util classes:** `PasswordUtil`, `CsrfUtil`, `IdGenerator` (all static methods)

### 5.3 Seat State Diagram

```
         ┌─────────────────────────────────────────────────┐
         │                                                 │
 hold()  │      expire() / releaseHold() / payment fail   │
AVAILABLE ──────────────► HELD ──────────────────────────► AVAILABLE
         │                 │                               │
         │ confirmPayment()│                               │
         │                 ▼                               │
         │               BOOKED ────── cancelBooking() ───┘
         └───────────────────────────────────────────────────
```

### 5.4 Sequence Diagram – Seat Hold Flow

```
User → Browser → HoldSeatsServlet → SeatService → (XML stores on pay)

1. User clicks "Hold Seats" → POST /user/holdSeats
2. AuthFilter validates session
3. HoldSeatsServlet reads showId, seatIds[]
4. SeatService.holdSeats(showId, seatIds, userId):
   a. lockForShow(showId).lock()
   b. purgeExpiredHolds()
   c. releaseUserHold() (if existing hold)
   d. validate all seats AVAILABLE
   e. mark all HELD, set expiresAt = now + 7min
   f. lock.unlock()
5. Redirect to /user/payment
```

### 5.5 Data Flow Diagram
*(Describe or draw Level-0 and Level-1 DFD)*

**External entities:** User, Admin, File System  
**Processes:** Authenticate, Browse/Search, Hold Seats, Process Payment, Manage Bookings  
**Data stores:** users.xml, movies.xml, shows.xml, seats.xml, bookings.xml

---

## 6. XML Schema (Data Model)

### users.xml
```xml
<users>
  <user>
    <id>u-alice01</id>
    <username>alice</username>
    <email>alice@example.com</email>
    <passwordHash>64-char hex string</passwordHash>
    <salt>32-char hex string</salt>
    <role>USER | ADMIN</role>
  </user>
</users>
```

### movies.xml
```xml
<movies>
  <movie>
    <id>m-inc001</id>
    <title>Inception</title>
    <genre>Sci-Fi</genre>
    <description>...</description>
    <durationMinutes>148</durationMinutes>
    <language>English</language>
    <rating>U/A</rating>
    <posterUrl></posterUrl>
  </movie>
</movies>
```

### shows.xml
```xml
<shows>
  <show>
    <id>sh-inc001</id>
    <movieId>m-inc001</movieId>
    <movieTitle>Inception</movieTitle>
    <showDateTime>2027-06-15T10:30:00</showDateTime>
    <pricePerSeat>200.0</pricePerSeat>
    <hallName>Hall 1</hallName>
  </show>
</shows>
```

### seats.xml (BOOKED only)
```xml
<seats>
  <seat>
    <showId>sh-inc001</showId>
    <seatId>A1</seatId>
    <state>BOOKED</state>
    <bookingId>BK-00001</bookingId>
  </seat>
</seats>
```

### bookings.xml
```xml
<bookings>
  <booking>
    <id>BK-00001</id>
    <userId>u-alice01</userId>
    <showId>sh-inc001</showId>
    <movieTitle>Inception</movieTitle>
    <showDateTime>2027-06-15T10:30:00</showDateTime>
    <seatIds>A1,A2</seatIds>
    <totalPrice>400.0</totalPrice>
    <status>CONFIRMED | CANCELLED</status>
    <bookedAt>2026-10-31T12:00:00</bookedAt>
  </booking>
</bookings>
```

---

## 7. Concurrency Design

*(Refer to VIVA_NOTES.md Section 4 for full detail)*

Key points to cover:
- Race condition example with two threads and seat A1
- Solution: `ConcurrentHashMap<showId, ReentrantLock>`
- Why per-show (not global) locks
- All-or-nothing hold implementation
- Dual expiry: lazy + scheduled
- Server-side expiry enforcement
- XML atomic writes (temp file + rename)
- `AtomicLong` for booking IDs

---

## 8. Implementation

### 8.1 Key Code Snippets

Include the following in your report:

1. **`SeatService.holdSeats()` method** – shows lock acquisition, expiry purge, validation loop, and marking
2. **`XmlStore.writeDocument()`** – shows temp-file-then-rename pattern
3. **`PasswordUtil.hash()`** – shows SHA-256 with salt
4. **`AppContextListener.contextInitialized()`** – shows ScheduledExecutorService setup
5. **`ConcurrentHoldTest.java`** – shows CountDownLatch pattern and assertions

### 8.2 Security Implementation
- SHA-256 + random salt (PasswordUtil)
- CSRF token in every mutating form (CsrfUtil)
- `c:out` in all JSPs (XSS prevention)
- HttpOnly session cookie (web.xml)
- Input validation in servlets and AuthService

---

## 9. Testing

### 9.1 Test Strategy
Unit testing of the core service layer using JUnit 5. Tests run against an in-memory `SeatService` with no-op XML stores (temp files) and a controllable `MutableClock`.

### 9.2 Test Results

| Test | Threads | Expected | Actual | Pass? |
|---|---|---|---|---|
| ConcurrentHoldTest | 50 | 1 success | [Fill] | ✅ |
| ConcurrentDifferentSeatsTest | 40 | 40 success | [Fill] | ✅ |
| MultiSeatAtomicityTest | 2 | 1 winner | [Fill] | ✅ |
| HoldExpiryTest | 1 | Expired hold rejected | [Fill] | ✅ |
| PaymentFailureTest | 1 | Seats released | [Fill] | ✅ |
| DoubleBookingTest | 1 | Second hold fails | [Fill] | ✅ |
| UnsafeSeatServiceDemoTest | 50 | Unsafe: >1 wins | [Fill] | ✅ |

### 9.3 Screenshots to Capture
*(Run `mvn cargo:run` and take screenshots of:)*
1. Login page
2. Movie list (3 movies shown)
3. Show timings for a movie
4. Seat selection grid (mix of available/held/booked)
5. Payment page with countdown timer
6. Booking confirmation page
7. My Bookings page
8. Admin Dashboard
9. Console output from `mvn test` (showing test counts)
10. Two-browser race demo (one success, one failure)

---

## 10. Limitations

1. **Single-JVM concurrency only** – locks don't work across multiple servers
2. **XML storage** – not suitable for high-traffic production use
3. **No email notifications** – confirmation is on-screen only
4. **Mock payment** – no real payment gateway integration
5. **SHA-256 hashing** – not memory-hard; bcrypt/Argon2id preferred in production
6. **In-memory hold state** – lost on server restart

---

## 11. Future Scope

| Enhancement | Technology |
|---|---|
| Real payment gateway | Razorpay / Stripe API |
| Multiple theatres support | Extended data model |
| Database backend | MySQL + JDBC or JPA |
| Distributed locking | Redis RedLock |
| Email confirmation | JavaMail / SendGrid |
| Mobile application | Android / React Native |
| Real-time updates | WebSockets (JSR-356) instead of polling |
| Reporting & analytics | Admin dashboard with charts (Chart.js) |

---

## 12. Conclusion

This project successfully demonstrates the design and implementation of a concurrent online movie ticket booking system using core Java web technologies. The primary contribution is the **concurrency architecture**: per-show `ReentrantLock` objects ensure double-booking is impossible, while dual-layer hold expiry (lazy + scheduled) keeps the system both correct and responsive.

All seven JUnit 5 tests pass, including the 50-thread concurrent hold test that mathematically proves only one thread can hold a given seat at a time. The deliberate "unsafe" demo test visually shows the race condition that locking eliminates — making it an excellent viva teaching tool.

The project meets all functional and non-functional requirements stated in the proposal and serves as a solid foundation for a production-grade booking system.

---

## 13. References

1. Oracle. (2024). *Java SE 17 Documentation*. https://docs.oracle.com/en/java/javase/17/
2. Jakarta EE. (2022). *Jakarta Servlet 6.0 Specification*. https://jakarta.ee/specifications/servlet/6.0/
3. Apache Software Foundation. (2023). *Apache Tomcat 10.1 Documentation*. https://tomcat.apache.org/tomcat-10.1-doc/
4. Apache Software Foundation. (2024). *Maven Documentation*. https://maven.apache.org/guides/
5. JUnit Team. (2024). *JUnit 5 User Guide*. https://junit.org/junit5/docs/current/user-guide/
6. Goetz, B., et al. (2006). *Java Concurrency in Practice*. Addison-Wesley.
7. OWASP. (2024). *Cross-Site Request Forgery (CSRF) Prevention Cheat Sheet*. https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
8. OWASP. (2024). *Password Storage Cheat Sheet*. https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
