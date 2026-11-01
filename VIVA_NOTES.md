# VIVA NOTES
## Online Movie Ticket Booking System
### Architecture, Concurrency Design & 20 Likely Viva Questions

---

## 1. Architecture Overview

```
Browser
  │  HTTP Requests
  ▼
┌─────────────────────────────────────────────────────────────┐
│                  Apache Tomcat 10.1                         │
│                                                             │
│  AuthFilter ──► Servlet ──► Service ──► Store ──► XML File │
│  AdminFilter          (SeatService)  (XmlStore)             │
│                        (AuthService)                        │
│                                                             │
│  AppContextListener (startup / shutdown)                    │
│  ScheduledExecutorService (hold expiry every 10s)           │
└─────────────────────────────────────────────────────────────┘
```

**Request Flow (Seat Hold Example):**
1. User selects seats → clicks "Hold" → `POST /user/holdSeats`
2. `AuthFilter` checks session → passes request to `HoldSeatsServlet`
3. `HoldSeatsServlet` reads parameters → calls `SeatService.holdSeats()`
4. `SeatService` acquires per-show `ReentrantLock` → checks availability → marks HELD
5. On payment: `SeatService.confirmPayment()` → marks BOOKED → `SeatStore.markBooked()` → `BookingStore.save()`
6. XML files updated atomically (write to `.tmp` → rename)

---

## 2. Package Responsibilities

| Package | Responsibility |
|---|---|
| `model` | Plain Java objects (POJOs): User, Movie, Show, Seat, Booking |
| `service` | Business logic: SeatService (holds/payments), AuthService (login/register) |
| `store` | XML file I/O: one Store class per XML file, each with its own ReadWriteLock |
| `servlet` | HTTP layer: reads request params, calls services, forwards to JSP |
| `filter` | Cross-cutting concerns: authentication (AuthFilter), role check (AdminFilter) |
| `listener` | Lifecycle: initialise singletons at startup, start/stop scheduler |
| `util` | Stateless helpers: PasswordUtil, CsrfUtil, IdGenerator |

---

## 3. Seat State Machine

```
                   ┌─────────────────────────────────────────┐
                   │                                         │
         hold()    │          expire / releaseHold()         │
AVAILABLE ──────► HELD ──────────────────────────────────► AVAILABLE
   ▲               │                                         │
   │               │ confirmPayment() (within expiry)        │
   │               ▼                                         │
   │             BOOKED ─────── cancelBooking() ────────────┘
   │
   └──────────── (BOOKED state is also AVAILABLE after cancel)
```

**Rules:**
- `AVAILABLE → HELD`: only if ALL requested seats are available (all-or-nothing)
- `HELD → BOOKED`: only if hold still valid (not expired) AND belongs to requesting user
- `HELD → AVAILABLE`: on expiry, payment failure, or explicit cancel
- `BOOKED → AVAILABLE`: only via explicit booking cancellation (future shows only)

---

## 4. Concurrency Design

### 4.1 The Core Race Condition

Without synchronisation, two threads can both see a seat as AVAILABLE and both hold it:

```
Time →  Thread A            Thread B
t1      read seat A1=AVAILABLE
t2                          read seat A1=AVAILABLE   ← both see available!
t3      mark A1=HELD(A)
t4                          mark A1=HELD(B)          ← double hold!
```

### 4.2 Fix: Per-Show ReentrantLock

```java
// ConcurrentHashMap<showId, ReentrantLock>
ReentrantLock lock = showLocks.computeIfAbsent(showId, k -> new ReentrantLock());
lock.lock();
try {
    // ALL of: purge expired, check availability, mark HELD
    // runs atomically – only ONE thread at a time per show
} finally {
    lock.unlock(); // ALWAYS release, even on exception
}
```

### 4.3 Why Per-Show Locks (Not One Global Lock)?

| Approach | Problem |
|---|---|
| Single global lock | All users on ALL shows queue behind each other – poor throughput |
| Per-show lock | Only users booking the SAME show contend – independent shows run in parallel |

In a multiplex with 10 screens running simultaneously, per-show locks mean operations on Screen 1 never block Screen 2 operations.

### 4.4 Why ConcurrentHashMap?

`ConcurrentHashMap.computeIfAbsent()` is thread-safe for creating the lock objects. Two threads accessing a new showId simultaneously both call `computeIfAbsent()` – exactly one `ReentrantLock` is created. This avoids a chicken-and-egg problem (you need a lock to safely create a lock).

### 4.5 All-Or-Nothing Hold (Atomicity)

Inside the show lock:
1. Purge expired holds (lazy expiry)
2. Check **every** requested seat is AVAILABLE
3. If **any** seat is unavailable → return failure, mark **nothing**
4. If **all** seats OK → mark **all** HELD in one pass

This prevents partial holds like "got A1 and A2 but A3 was taken" – the user gets all or none.

### 4.6 Hold Expiry: Two Layers

**Lazy expiry** (every read/write): Before treating any seat as HELD, check `seat.isHoldExpired(now)`. If true, reset to AVAILABLE immediately. Correctness never depends on the scheduler.

**Scheduled expiry** (every 10 seconds): `ScheduledExecutorService` runs `purgeAllExpiredHolds()` so the seat grid visually updates for other users quickly.

**WHY BOTH?**
- Scheduler might be delayed (GC pause, CPU load) → lazy expiry is the safety net
- Without the scheduler, a seat might appear orange (HELD) for up to 7 minutes even after expiry → bad UX
- Together: correct AND responsive

### 4.7 Why Server-Side Expiry (Not Browser Timer)?

The browser countdown is purely cosmetic. The browser can be:
- Modified by the user (JS injection)
- Running on a machine with wrong clock
- Slow/paused

`SeatService.confirmPayment()` re-checks `holdExpiresAt` against `clock.millis()` **on the server**. Even if the user submits the form after expiry (manually or via a bug), the server rejects it.

### 4.8 XML Atomic Writes

```
WRONG (crash-safe):   write directly to users.xml  → half-written file on crash!
RIGHT  (atomic):      write to users.xml.tmp → rename/move over users.xml
```

OS `rename()` is atomic on most file systems (POSIX guarantees). Readers see either the old complete file or the new complete file – never a partial write.

Java: `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`

### 4.9 ReadWriteLock on XML Files

```
Multiple readers: allowed simultaneously (no contention when just reading)
Single writer:    exclusive (no readers can read during a write)
```

A `ReentrantReadWriteLock` per XML file prevents a writer from corrupting a file that a reader is currently parsing.

### 4.10 AtomicLong for Booking IDs

```java
private static final AtomicLong COUNTER = new AtomicLong(1);
public static String nextBookingId() {
    return String.format("BK-%05d", COUNTER.getAndIncrement());
}
```

`getAndIncrement()` is a single atomic CPU instruction (Compare-And-Swap). Two threads calling it simultaneously get different values – no lock needed.

### 4.11 Clock Injection for Testing

```java
// Production:  Clock.systemUTC()   → real time
// Tests:       new MutableClock()  → controllable fake time
```

By injecting `java.time.Clock`, tests can advance time 7 minutes in a single line without actually sleeping. This proves the server enforces expiry correctly without a 7-minute wait.

---

## 5. Known Limitations (for Viva)

| Limitation | Real-World Solution |
|---|---|
| Locks only work within one JVM | Redis RedLock, ZooKeeper, or DB row-level locks for multi-server |
| XML is slow for large data | Relational DB (MySQL/PostgreSQL) with ACID transactions |
| No email notifications | JavaMail / SMTP integration |
| No real payment | Razorpay/Stripe payment gateway integration |
| SHA-256 password hashing | Use bcrypt (cost ≥12) or Argon2id in production |
| Hold state lost on restart | Persist holds to DB with TTL; or Redis with expiry |

---

## 6. 20 Likely Viva Questions & Short Answers

---

**Q1. Why use Servlets and JSP instead of Spring MVC?**  
A: Spring MVC is built on top of Servlets. Understanding raw Servlets gives insight into how any web framework works under the hood. For a university project, it demonstrates foundational knowledge without framework magic.

---

**Q2. What is the role of `AppContextListener`?**  
A: It implements `ServletContextListener`. `contextInitialized()` runs when Tomcat starts: creates all service/store singletons, loads seed XML data, initialises seat grids, and starts the hold-expiry `ScheduledExecutorService`. `contextDestroyed()` shuts down the scheduler gracefully.

---

**Q3. Why store passwords as `SHA-256(salt + password)` and not plain text?**  
A: Plain text is catastrophic if the XML file is stolen. SHA-256 is a one-way hash – you can't reverse it to get the password. The random salt per user ensures two users with the same password have different hashes (prevents rainbow-table attacks).

---

**Q4. What is a CSRF attack? How does the app prevent it?**  
A: A malicious website submits a form to our server on behalf of a logged-in user (e.g., cancels their bookings). Prevention: every mutating form includes a hidden `_csrf` field containing a random session-bound token. The server rejects any request where the submitted token doesn't match the session token.

---

**Q5. What is a race condition? Give a specific example from this project.**  
A: A race condition occurs when the result of an operation depends on the timing of multiple threads. Example: Thread A and Thread B both read seat A1 as AVAILABLE. Both pass the availability check. Both mark it HELD. Two users hold the same seat – double booking.

---

**Q6. How does `ReentrantLock` prevent the race condition?**  
A: `lock.lock()` allows only ONE thread at a time to enter the critical section for that show. The second thread blocks at `lock.lock()` until the first calls `lock.unlock()`. By then, seat A1 is HELD, so the second thread's availability check fails and it returns an error.

---

**Q7. Why `try/finally` when using locks?**  
A: If an exception is thrown inside the lock, without `finally` the lock is never released. All other threads for that show would block forever – a deadlock. `finally` guarantees `lock.unlock()` runs even if there's an exception.

---

**Q8. Why per-show locks instead of one global lock?**  
A: A global lock means all users booking any show must queue behind each other, serialising the entire system. Per-show locks only serialise users on the SAME show. Users on different shows run in parallel. Much better throughput for a multiplex.

---

**Q9. What is lazy expiry? Why do you also have a scheduler?**  
A: Lazy expiry: every read/write checks `seat.isHoldExpired(now)` and resets expired holds immediately, without waiting for any scheduler. This ensures correctness. The scheduler (every 10s) is purely for UX: it proactively releases holds so the seat grid shows green quickly for other users. Without it, the grid might show a seat as orange for up to 7 minutes even after expiry.

---

**Q10. Why does the browser countdown NOT enforce expiry?**  
A: The browser can be manipulated (JavaScript injection, wrong system clock, slow computer). The `confirmPayment()` servlet checks `holdExpiresAt` on the server against `clock.millis()`. Even if the user sends the payment form after the hold expired, the server rejects it. The browser timer is a UX feature, not a security mechanism.

---

**Q11. What is an atomic write? How is it implemented here?**  
A: A write where the file is either fully written or not at all – never partial. Implementation: write to `filename.tmp`, then `Files.move(tmp, target, ATOMIC_MOVE)`. The OS rename is atomic, so readers see either the old complete file or the new complete file. A crash during write only corrupts the temp file, not the real data.

---

**Q12. What is a `ReadWriteLock`? Why use it for XML files?**  
A: A `ReentrantReadWriteLock` allows multiple concurrent readers OR one exclusive writer (never both). For XML files: multiple threads can read (parse) simultaneously without blocking each other, but only one thread can write at a time, and no reads happen during a write. Better throughput than a plain mutual exclusion lock.

---

**Q13. What is `ConcurrentHashMap` and why use it for `showLocks`?**  
A: A thread-safe version of HashMap. Its `computeIfAbsent()` is atomic: if two threads race to create a lock for the same showId, exactly one `ReentrantLock` is created. Without `ConcurrentHashMap`, two threads might create two lock objects for the same show – then there would be no mutual exclusion.

---

**Q14. What is `AtomicLong` and why use it for booking IDs?**  
A: `AtomicLong` uses CPU-level Compare-And-Swap (CAS) instructions. `getAndIncrement()` reads the current value, increments it, and writes back atomically in a single instruction – no two threads can interleave. Without it, two threads could read the same counter value and generate duplicate booking IDs.

---

**Q15. Explain the all-or-nothing hold. Why is it necessary?**  
A: When a user selects multiple seats (e.g., A1, A2, A3), we either hold ALL of them or NONE. If we held A1 and A2 but A3 was taken by someone else, the user would be stuck with a partial group and couldn't sit together. The server checks all seats first, then marks all HELD only if every seat passes.

---

**Q16. How does the seat grid refresh every 5 seconds?**  
A: JavaScript (`seat-grid.js`) uses `setInterval()` to call `fetch('/user/seatGrid?showId=...')` every 5000ms. The server endpoint (`SeatGridFragmentServlet`) returns a JSON array of seat states. The JS then updates the CSS classes on each seat button without reloading the page.

---

**Q17. What is JSTL `c:out`? Why use it instead of `${variable}` directly?**  
A: `c:out` HTML-escapes the output. For example, if a username is `<script>alert(1)</script>`, `${username}` would inject that script into the page (XSS attack). `<c:out value="${username}"/>` renders it as literal text `&lt;script&gt;...`. Always use `c:out` for user-controlled data.

---

**Q18. How does session-based authentication work?**  
A: After login, the `User` object is stored in `HttpSession` (`session.setAttribute("loggedInUser", user)`). The session is identified by a cookie (`JSESSIONID`) sent to the browser. `AuthFilter` checks `session.getAttribute("loggedInUser")` on every protected request. Logout calls `session.invalidate()` to destroy the session.

---

**Q19. What happens if the server restarts while a user is in the middle of a hold?**  
A: Held (not booked) seats are in-memory only. On restart, all in-memory state is lost. The seat goes back to AVAILABLE (no HELD record exists in seats.xml). The user's session is also invalidated. They must log in and hold again. This is an accepted limitation documented in the README.

---

**Q20. How would you make this system work across multiple servers (horizontal scaling)?**  
A: `ReentrantLock` only works within one JVM. For multiple servers:
- **Distributed locks**: Redis RedLock or Apache ZooKeeper
- **Database transactions**: Use `SELECT FOR UPDATE` row-level locking in MySQL/PostgreSQL
- **Event sourcing**: Commands go to a single message queue; one consumer processes them sequentially
- **Session sharing**: Store sessions in Redis so any server can validate them

This is the primary real-world limitation of our in-memory locking approach.
