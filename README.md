# Screenly – Online Movie Ticket Booking
### University PBL Project – Java 17 | Servlets | JSP | XML | JUnit 5

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Default Logins](#default-logins)
4. [Prerequisites](#prerequisites)
5. [Setup & Build](#setup--build)
6. [Running the Application](#running-the-application)
7. [Running the Tests](#running-the-tests)
8. [Project Structure](#project-structure)
9. [Manual Demo Script (Two-Browser Race)](#manual-demo-script-two-browser-race)
10. [Known Limitations](#known-limitations)

---

## Project Overview
**Screenly** is a full-featured web application for booking movie tickets online.  
Key features: user registration/login, movie listing with search and genre filtering, show timings, interactive seat selection with live updates, 7-minute hold-then-pay flow, booking management, and an admin panel.

**Core concurrency design:** per-show `ReentrantLock` prevents double-booking; `ScheduledExecutorService` + lazy expiry releases timed-out holds; atomic XML writes via temp-file-then-rename.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Web Layer | Jakarta Servlets 6.0, JSP, JSTL 3.0 |
| Data Storage | XML files (DOM parser – `javax.xml.parsers`) |
| Build Tool | Apache Maven 3.x (WAR packaging) |
| Server | Apache Tomcat 10.1 (embedded via Cargo plugin) |
| Tests | JUnit 5.10 |
| Security | SHA-256 + salt passwords, CSRF tokens, HttpOnly cookies |
| UI | Minimal Dark Cinema design system (`screenly.css`), Inter font, plain JS |

---

## Default Logins

| Role | Username | Password |
|---|---|---|
| **ADMIN** | `admin` | `admin123` |
| **USER** | `alice` | `alice123` |
| **USER** | `bob` | `bob12345` |

---

## Prerequisites

| Requirement | Minimum Version | Check Command |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Apache Maven | 3.6 | `mvn -version` |
| Internet | (first build downloads deps) | — |

> **No database required.** All data is stored in XML files under `src/main/resources/data/`.

---

## Setup & Build

```bash
# 1. Clone or extract the project
cd "Java Project 5th Sem"

# 2. Build (compile + package WAR, skip tests for speed)
mvn clean package -DskipTests

# 3. Build AND run all tests
mvn clean test
```

The WAR file is produced at: `target/screenly.war`

---

## Running the Application

### Option A – Embedded Tomcat via Cargo (Recommended)

```bash
mvn cargo:run
```

Open your browser: **http://localhost:8080/screenly**

Press `Ctrl+C` to stop.

### Option B – Deploy to standalone Tomcat 10.1

```bash
# 1. Build WAR
mvn clean package -DskipTests

# 2. Copy to Tomcat's webapps directory
cp target/screenly.war /path/to/tomcat10/webapps/

# 3. Start Tomcat
/path/to/tomcat10/bin/startup.sh   # Linux/Mac
/path/to/tomcat10/bin/startup.bat  # Windows

# 4. Tell the app where data files live (add to CATALINA_OPTS)
export CATALINA_OPTS="-Dmovietix.data.dir=/absolute/path/to/src/main/resources/data"
```

Open: **http://localhost:8080/screenly**

---

## Running the Tests

```bash
# Run all 8 JUnit tests and print results
mvn clean test
```

### What Each Test Does

| Test Class | Description | Expected |
|---|---|---|
| `ConcurrentHoldTest` | 50 threads race for seat A1 | 1 success, 49 failures |
| `ConcurrentDifferentSeatsTest` | 40 threads hold 40 different seats | 40 successes |
| `MultiSeatAtomicityTest` | User A (A1-A3) vs User B (A3-A5) | 1 winner gets all 3; other gets 0 |
| `HoldExpiryTest` | Fake clock advances 7m+1s | Expired hold → AVAILABLE; late payment rejected |
| `PaymentFailureTest` | Hold then fail payment | Seats return to AVAILABLE |
| `DoubleBookingTest` | Book seat then try again | Second hold attempt rejected |
| `UnsafeSeatServiceDemoTest` | Unsafe (no lock) vs Safe (locked) | Safe: always 1 win; Unsafe: usually >1 |

### Viewing Test Output (Verbose)

```bash
mvn clean test -Dsurefire.useFile=false
```

The 50-thread test prints a summary table to the console — useful for the viva demonstration.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/movietix/
│   │   ├── model/          # User, Movie, Show, Seat, Booking (POJOs)
│   │   ├── service/        # SeatService (core locking), AuthService
│   │   ├── store/          # XmlStore + 5 entity stores (XML read/write)
│   │   ├── servlet/        # 15 HTTP Servlets
│   │   ├── filter/         # AuthFilter, AdminFilter
│   │   ├── listener/       # AppContextListener (startup/shutdown)
│   │   └── util/           # PasswordUtil, CsrfUtil, IdGenerator
│   ├── resources/data/     # XML data files (seed + runtime)
│   └── webapp/
│       ├── WEB-INF/
│       │   ├── web.xml
│       │   └── views/      # 12 JSP pages + base.jspf
│       ├── static/
│       │   ├── css/        # screenly.css (design system), style.css (forwards)
│       │   └── js/         # countdown.js, seat-grid.js, movies.js
│       └── images/
│           └── posters/    # Movie poster SVGs (inception, rrr, interstellar)
└── test/java/com/movietix/test/   # 8 JUnit 5 test classes
```

---

## Manual Demo Script (Two-Browser Race)

> **Goal:** Show two users trying to book the same seat simultaneously.

### Setup
1. Start the app: `mvn cargo:run`
2. Open **Browser 1** (normal window): http://localhost:8080/screenly
3. Open **Browser 2** (incognito/private window): http://localhost:8080/screenly

### Steps

**Browser 1:** Login as `alice` / `alice123`  
**Browser 2:** Login as `bob` / `bob12345`

1. Both navigate to the same movie → same show → seat selection page.
2. **Both click the same seat** (e.g. `A1`) — it should turn red/selected for each user locally.
3. **Both click "Hold Seats"** as quickly as possible (within 1-2 seconds of each other).
4. **Expected result:**
   - One browser goes to the Payment page ✅
   - Other browser shows: *"Seat A1 is not available (state=HELD)"* ❌
5. In Browser 1, click **"Simulate Payment Failure"** → watch seat A1 turn back to **available** in Browser 2 (within 5 seconds, via polling).
6. Now Browser 2 can hold A1.

### What to explain in viva
- The per-show `ReentrantLock` ensures only one thread executes the check-then-hold atomically.
- The 5-second JavaScript polling (`/user/seatGrid`) is what makes the colour update visible.
- Payment failure calls `releaseHold()` which resets seat state to `AVAILABLE`.

---

## Known Limitations

1. **Single JVM only** – `ReentrantLock` works within one JVM. A load-balanced multi-server deployment would need distributed locks (e.g., Redis RedLock) or database row-level locks.
2. **No real payment gateway** – Payment is mocked with two buttons.
3. **XML is slow at scale** – Suitable for a university project; production would use a relational database.
4. **No email confirmation** – Booking confirmation is shown on screen only.
5. **Hold state lost on restart** – Held (not booked) seats are in-memory; a server restart clears all holds (by design).
# Screenly
