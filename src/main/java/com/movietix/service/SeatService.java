package com.movietix.service;

import com.movietix.model.Booking;
import com.movietix.model.Booking.BookingStatus;
import com.movietix.model.Seat;
import com.movietix.model.Seat.SeatState;
import com.movietix.model.Show;
import com.movietix.store.BookingStore;
import com.movietix.store.SeatStore;
import com.movietix.util.IdGenerator;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * =========================================================================
 * SeatService – The Heart of the Booking System
 * =========================================================================
 *
 * This singleton manages all seat state in memory and enforces all business
 * rules around holding, paying, releasing and cancelling seats.
 *
 * =========================================================================
 * CONCURRENCY DESIGN – read this before touching this class (viva-critical)
 * =========================================================================
 *
 * PROBLEM: Servlets handle requests in separate threads.  Two users can
 * simultaneously try to hold the same seat.  Without synchronisation, the
 * following race condition occurs:
 *
 *   Thread A: reads seat A1 → AVAILABLE
 *   Thread B: reads seat A1 → AVAILABLE  ← both read AVAILABLE!
 *   Thread A: marks A1 as HELD by user A
 *   Thread B: marks A1 as HELD by user B  ← DOUBLE HOLD BUG
 *
 * SOLUTION: A ConcurrentHashMap keyed by showId, where each value is a
 * ReentrantLock.  Before any check-then-act operation (hold, pay, release,
 * cancel) we acquire that show's lock.
 *
 *   showLocks.computeIfAbsent(showId, k -> new ReentrantLock()).lock()
 *
 * WHY per-show locks (not a single global lock)?
 *   - A global lock would force all users on ALL shows to queue behind each
 *     other, destroying throughput for a multiplex with many shows.
 *   - Per-show locks mean threads on different shows never contend.
 *   - Contention only occurs when two users pick seats on the SAME show
 *     at the same instant, which is rare in practice.
 *
 * PATTERN: Every critical section uses try/finally to guarantee unlock even
 * if an exception is thrown.  Forgetting to unlock = deadlock.
 *
 * =========================================================================
 * LAZY EXPIRY + SCHEDULED EXPIRY (belt-and-suspenders)
 * =========================================================================
 *
 * Holds expire after HOLD_DURATION_MILLIS (7 minutes).
 *
 * We enforce expiry in TWO ways:
 *
 *  1. LAZY EXPIRY (every read): before treating a seat as HELD, we check
 *     seat.isHoldExpired(now).  If true, we reset it to AVAILABLE in-place.
 *     This means correctness never depends on the scheduler firing.
 *
 *  2. SCHEDULED EXPIRY (every 10 seconds): HoldExpiryTask runs inside the
 *     per-show lock and proactively releases expired holds so the seat
 *     appears green in the grid to other users quickly.
 *
 * WHY BOTH?  The scheduler might not run exactly on time (GC pause, load).
 * Lazy expiry is the true guard.  The scheduler just keeps the grid fresh.
 *
 * =========================================================================
 * HOLD: ALL-OR-NOTHING (atomic)
 * =========================================================================
 *
 * For multi-seat holds:
 *   1. Purge expired holds (lazy).
 *   2. Verify EVERY requested seat is AVAILABLE (or HELD by the same user).
 *   3. If all OK → mark all HELD in a single lock window.
 *   4. If any seat is unavailable → mark NONE (return failure).
 *
 * This prevents partial holds like "A1 held but A2 failed".
 *
 * =========================================================================
 * CLOCK INJECTION (for testing without sleeping 7 minutes)
 * =========================================================================
 *
 * Instead of calling System.currentTimeMillis() directly, we use an
 * injected java.time.Clock.  Tests pass a fake/adjustable clock so they
 * can advance time without actually waiting.
 */
public class SeatService {

    private static final Logger LOG = Logger.getLogger(SeatService.class.getName());

    /** Hold duration: 7 minutes in milliseconds. */
    public static final long HOLD_DURATION_MILLIS = 7 * 60 * 1000L;

    /** Seat grid: rows A-E, columns 1-8. */
    private static final String[] ROWS    = {"A", "B", "C", "D", "E"};
    private static final int      COLUMNS = 8;

    // -------------------------------------------------------------------------
    // In-memory seat state: Map<showId, Map<seatId, Seat>>
    // The outer ConcurrentHashMap is safe for concurrent reads/writes at the
    // map level. The inner map for each show is only accessed while holding
    // that show's ReentrantLock, so it can be a plain HashMap.
    // -------------------------------------------------------------------------
    private final ConcurrentHashMap<String, Map<String, Seat>> seatGrid
        = new ConcurrentHashMap<>();

    /**
     * Per-show locks.
     *
     * WHY: Ensures only one thread at a time mutates the seat grid for a
     * particular show, preventing double-holds and double-bookings.
     * Using ConcurrentHashMap.computeIfAbsent ensures only one lock object
     * is created per showId even under concurrent first-access.
     */
    private final ConcurrentHashMap<String, ReentrantLock> showLocks
        = new ConcurrentHashMap<>();

    /**
     * Tracks each user's active hold: Map<userId, holdContext>.
     * Guarded by the show's ReentrantLock when written.
     * Read under the same lock before allowing a new hold.
     */
    private final ConcurrentHashMap<String, HoldContext> userActiveHolds
        = new ConcurrentHashMap<>();

    /** Injected clock – use this.clock.millis() instead of System.currentTimeMillis(). */
    private final Clock clock;

    private final SeatStore    seatStore;
    private final BookingStore bookingStore;

    // -------------------------------------------------------------------------

    public SeatService(Clock clock, SeatStore seatStore, BookingStore bookingStore) {
        this.clock        = clock;
        this.seatStore    = seatStore;
        this.bookingStore = bookingStore;
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Called once at application startup (from AppContextListener).
     * For each show, initialises an in-memory grid of all 40 seats as AVAILABLE,
     * then overlays BOOKED seats from seats.xml.
     */
    public void initShow(String showId) {
        ReentrantLock lock = lockForShow(showId);
        lock.lock();
        try {
            Map<String, Seat> grid = new HashMap<>();
            // Generate all 40 seat IDs: A1..A8, B1..B8, …, E8
            for (String row : ROWS) {
                for (int col = 1; col <= COLUMNS; col++) {
                    String seatId = row + col;
                    grid.put(seatId, new Seat(showId, seatId)); // AVAILABLE by default
                }
            }
            seatGrid.put(showId, grid);
            LOG.info("Initialised empty seat grid for show " + showId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Overlays persisted BOOKED seats on the in-memory grid.
     * Called after initShow, during startup.
     */
    public void loadBookedSeats() {
        seatStore.findAllBooked().forEach(bookedSeat -> {
            Map<String, Seat> grid = seatGrid.get(bookedSeat.getShowId());
            if (grid != null) {
                Seat inMem = grid.get(bookedSeat.getSeatId());
                if (inMem != null) {
                    inMem.setState(SeatState.BOOKED);
                    inMem.setBookingId(bookedSeat.getBookingId());
                }
            }
        });
        LOG.info("Loaded booked seats from XML.");
    }

    // -------------------------------------------------------------------------
    // HOLD SEATS
    // -------------------------------------------------------------------------

    /**
     * Result returned by holdSeats / confirmPayment / releaseHold / cancelBooking.
     *
     * @param success true if the operation succeeded
     * @param message human-readable result (displayed on UI)
     * @param bookingId non-null only on successful confirmPayment
     */
    public record OperationResult(boolean success, String message, String bookingId) {
        public OperationResult(boolean success, String message) {
            this(success, message, null);
        }
    }

    /**
     * Attempts to hold the given seats for the specified user on a show.
     *
     * Thread-safety: acquires the per-show ReentrantLock. The entire
     * check-then-act sequence (purge expired → verify all available → mark held)
     * runs atomically under this lock.
     *
     * ALL-OR-NOTHING: if any requested seat is unavailable, no seats are held.
     *
     * @param showId   target show
     * @param seatIds  list of seat IDs to hold (max 6 enforced by caller)
     * @param userId   the user requesting the hold
     * @return OperationResult indicating success or the reason for failure
     */
    public OperationResult holdSeats(String showId, List<String> seatIds, String userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            return new OperationResult(false, "No seats selected.");
        }
        if (seatIds.size() > 6) {
            return new OperationResult(false, "You can hold at most 6 seats at a time.");
        }

        ReentrantLock lock = lockForShow(showId);
        lock.lock();                              // ← ACQUIRE per-show lock
        try {
            Map<String, Seat> grid = seatGrid.get(showId);
            if (grid == null) {
                return new OperationResult(false, "Show not found: " + showId);
            }

            long now = clock.millis();

            // Step 1: Release any expired holds on this show (lazy expiry)
            purgeExpiredHolds(grid, now);

            // Step 2: Release this user's previous hold on ANY show (a user
            //         can only have one active hold across all shows)
            releaseUserHold(userId, now);

            // Step 3: Validate all requested seats
            for (String seatId : seatIds) {
                Seat seat = grid.get(seatId);
                if (seat == null) {
                    return new OperationResult(false, "Invalid seat: " + seatId);
                }
                // Lazy expiry check
                if (seat.isHoldExpired(now)) {
                    resetToAvailable(seat);
                }
                if (seat.getState() != SeatState.AVAILABLE) {
                    return new OperationResult(false,
                        "Seat " + seatId + " is not available (state=" + seat.getState() + ").");
                }
            }

            // Step 4: Mark all seats as HELD (all-or-nothing: we only reach here if all are OK)
            long expiresAt = now + HOLD_DURATION_MILLIS;
            for (String seatId : seatIds) {
                Seat seat = grid.get(seatId);
                seat.setState(SeatState.HELD);
                seat.setHoldOwnerUserId(userId);
                seat.setHoldExpiresAt(expiresAt);
            }

            // Step 5: Record this user's active hold
            userActiveHolds.put(userId, new HoldContext(showId, new ArrayList<>(seatIds), expiresAt));

            LOG.info("[" + Thread.currentThread().getName() + "] User " + userId +
                     " HELD seats " + seatIds + " on show " + showId +
                     " until " + new Date(expiresAt));
            return new OperationResult(true, "Seats held successfully. You have 7 minutes to pay.");

        } finally {
            lock.unlock();                        // ← RELEASE per-show lock (always)
        }
    }

    // -------------------------------------------------------------------------
    // CONFIRM PAYMENT (HELD → BOOKED)
    // -------------------------------------------------------------------------

    /**
     * Confirms payment for the current hold of the given user.
     *
     * Server-side expiry re-check: we verify that the hold still exists,
     * belongs to this user, and has NOT expired.  The browser countdown is
     * display-only; the server is the source of truth.
     *
     * @param showId show ID
     * @param userId the paying user
     * @param show   Show object (for denormalised data in booking record)
     * @return OperationResult with bookingId on success
     */
    public OperationResult confirmPayment(String showId, String userId, Show show) {
        ReentrantLock lock = lockForShow(showId);
        lock.lock();
        try {
            Map<String, Seat> grid = seatGrid.get(showId);
            if (grid == null) return new OperationResult(false, "Show not found.");

            long now = clock.millis();

            // Retrieve this user's hold context
            HoldContext ctx = userActiveHolds.get(userId);
            if (ctx == null || !ctx.showId().equals(showId)) {
                return new OperationResult(false, "No active hold found for this show. Please select seats again.");
            }
            if (now >= ctx.expiresAt()) {
                // Hold has expired; clean up
                ctx.seatIds().forEach(sid -> {
                    Seat s = grid.get(sid);
                    if (s != null && userId.equals(s.getHoldOwnerUserId())) resetToAvailable(s);
                });
                userActiveHolds.remove(userId);
                return new OperationResult(false, "Your hold has expired. Please select seats again.");
            }

            // Verify each seat is still HELD by this user
            for (String seatId : ctx.seatIds()) {
                Seat seat = grid.get(seatId);
                if (seat == null || seat.getState() != SeatState.HELD
                    || !userId.equals(seat.getHoldOwnerUserId())) {
                    return new OperationResult(false,
                        "Seat " + seatId + " is no longer held by you. Cannot complete payment.");
                }
            }

            // Generate booking ID
            String bookingId = IdGenerator.nextBookingId();
            double price     = ctx.seatIds().size() * show.getPricePerSeat();

            // Mark all seats BOOKED in memory
            for (String seatId : ctx.seatIds()) {
                Seat seat = grid.get(seatId);
                seat.setState(SeatState.BOOKED);
                seat.setHoldOwnerUserId(null);
                seat.setHoldExpiresAt(0);
                seat.setBookingId(bookingId);
            }

            // Remove user's active hold
            userActiveHolds.remove(userId);

            // Persist: seats.xml (booked seats) + bookings.xml (booking record)
            // These XML writes happen under the show lock so they are consistent
            // with the in-memory state.
            seatStore.markBooked(showId, ctx.seatIds(), bookingId);

            Booking booking = new Booking(
                bookingId, userId, showId,
                show.getMovieTitle(), show.getShowDateTime(),
                ctx.seatIds(), price,
                BookingStatus.CONFIRMED,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            bookingStore.save(booking);

            LOG.info("[" + Thread.currentThread().getName() + "] Payment CONFIRMED: booking " +
                     bookingId + " seats=" + ctx.seatIds() + " show=" + showId);
            return new OperationResult(true, "Payment successful! Booking confirmed.", bookingId);

        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // RELEASE HOLD (payment failure / cancel hold)
    // -------------------------------------------------------------------------

    /**
     * Releases the active hold of the given user (HELD → AVAILABLE).
     * Called on payment failure or explicit cancel.
     */
    public OperationResult releaseHold(String userId) {
        HoldContext ctx = userActiveHolds.get(userId);
        if (ctx == null) {
            return new OperationResult(false, "No active hold to release.");
        }

        ReentrantLock lock = lockForShow(ctx.showId());
        lock.lock();
        try {
            long now = clock.millis();
            Map<String, Seat> grid = seatGrid.get(ctx.showId());
            if (grid != null) {
                for (String seatId : ctx.seatIds()) {
                    Seat seat = grid.get(seatId);
                    if (seat != null && userId.equals(seat.getHoldOwnerUserId())) {
                        resetToAvailable(seat);
                    }
                }
            }
            userActiveHolds.remove(userId);
            LOG.info("[" + Thread.currentThread().getName() + "] RELEASED hold for user " + userId +
                     " seats=" + ctx.seatIds() + " show=" + ctx.showId());
            return new OperationResult(true, "Seats released.");
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // CANCEL BOOKING (BOOKED → AVAILABLE)
    // -------------------------------------------------------------------------

    /**
     * Cancels a confirmed booking: seats go back to AVAILABLE, booking status
     * becomes CANCELLED, seat records are removed from seats.xml.
     *
     * Only future shows can be cancelled (enforced by CancelBookingServlet).
     */
    public OperationResult cancelBooking(Booking booking) {
        ReentrantLock lock = lockForShow(booking.getShowId());
        lock.lock();
        try {
            Map<String, Seat> grid = seatGrid.get(booking.getShowId());
            if (grid != null) {
                for (String seatId : booking.getSeatIds()) {
                    Seat seat = grid.get(seatId);
                    if (seat != null) {
                        resetToAvailable(seat);
                    }
                }
            }
            // Update XML: remove seat records, update booking status
            seatStore.removeByBookingId(booking.getId());
            bookingStore.updateStatus(booking.getId(), BookingStatus.CANCELLED);

            LOG.info("[" + Thread.currentThread().getName() + "] CANCELLED booking " +
                     booking.getId() + " seats=" + booking.getSeatIds());
            return new OperationResult(true, "Booking cancelled. Mock refund processed.");
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // READ seat grid (for UI)
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the seat grid for a show.
     * Applies lazy expiry before returning so the grid is fresh.
     *
     * The returned list is a defensive copy (safe to send to JSP without locking).
     */
    public List<Seat> getSeatGrid(String showId) {
        ReentrantLock lock = lockForShow(showId);
        lock.lock();
        try {
            long now = clock.millis();
            Map<String, Seat> grid = seatGrid.get(showId);
            if (grid == null) return Collections.emptyList();

            purgeExpiredHolds(grid, now);

            // Build a defensive copy sorted by seatId (A1, A2, … E8)
            return grid.values().stream()
                .map(this::copySeat)
                .sorted(Comparator.comparing(Seat::getSeatId))
                .toList();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the user's active hold context (null if none).
     * Reading from ConcurrentHashMap is safe without additional locking.
     */
    public HoldContext getActiveHold(String userId) {
        return userActiveHolds.get(userId);
    }

    // -------------------------------------------------------------------------
    // Scheduled expiry – called by HoldExpiryTask every 10 seconds
    // -------------------------------------------------------------------------

    /**
     * Runs inside the per-show lock; purges expired holds for ALL shows.
     * This is the proactive (scheduled) expiry counterpart to lazy expiry.
     */
    public void purgeAllExpiredHolds() {
        long now = clock.millis();
        seatGrid.forEach((showId, grid) -> {
            ReentrantLock lock = lockForShow(showId);
            lock.lock();
            try {
                purgeExpiredHolds(grid, now);
            } finally {
                lock.unlock();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Gets-or-creates the ReentrantLock for a show. Thread-safe via computeIfAbsent. */
    private ReentrantLock lockForShow(String showId) {
        return showLocks.computeIfAbsent(showId, k -> new ReentrantLock());
    }

    /**
     * Scans the grid for expired holds and resets them to AVAILABLE.
     * MUST be called while holding the show's lock.
     */
    private void purgeExpiredHolds(Map<String, Seat> grid, long now) {
        for (Seat seat : grid.values()) {
            if (seat.isHoldExpired(now)) {
                String prevOwner = seat.getHoldOwnerUserId();
                resetToAvailable(seat);
                userActiveHolds.remove(prevOwner);
                LOG.info("[" + Thread.currentThread().getName() + "] EXPIRED hold on seat " +
                         seat.getSeatId() + " show=" + seat.getShowId() + " (was held by " + prevOwner + ")");
            }
        }
    }

    /**
     * If the user has an existing hold on ANY show, release it first.
     * This enforces the "one active hold per user" rule.
     * MUST be called while holding the NEW show's lock.
     * NOTE: If the old hold was on a different show, we acquire that show's lock
     * briefly to safely clear the seats.
     */
    private void releaseUserHold(String userId, long now) {
        HoldContext prev = userActiveHolds.get(userId);
        if (prev == null) return;

        Map<String, Seat> prevGrid = seatGrid.get(prev.showId());
        if (prevGrid != null) {
            if (!prev.showId().equals(/* current lock's showId */ prev.showId())) {
                // Different show – need a brief nested lock
                // (This cannot deadlock because we always acquire in request-order
                //  for the SAME user; a user can only hold one show at a time)
                ReentrantLock prevLock = lockForShow(prev.showId());
                prevLock.lock();
                try {
                    clearUserSeatsInGrid(prevGrid, userId);
                } finally {
                    prevLock.unlock();
                }
            } else {
                clearUserSeatsInGrid(prevGrid, userId);
            }
        }
        userActiveHolds.remove(userId);
        LOG.info("Released previous hold for user " + userId + " on show " + prev.showId());
    }

    private void clearUserSeatsInGrid(Map<String, Seat> grid, String userId) {
        for (Seat seat : grid.values()) {
            if (seat.getState() == SeatState.HELD && userId.equals(seat.getHoldOwnerUserId())) {
                resetToAvailable(seat);
            }
        }
    }

    /** Resets a seat to AVAILABLE state, clearing hold metadata. */
    private void resetToAvailable(Seat seat) {
        seat.setState(SeatState.AVAILABLE);
        seat.setHoldOwnerUserId(null);
        seat.setHoldExpiresAt(0);
        seat.setBookingId(null);
    }

    /** Creates a shallow copy of a Seat (so the caller's view is immutable). */
    private Seat copySeat(Seat original) {
        Seat copy = new Seat(original.getShowId(), original.getSeatId());
        copy.setState(original.getState());
        copy.setHoldOwnerUserId(original.getHoldOwnerUserId());
        copy.setHoldExpiresAt(original.getHoldExpiresAt());
        copy.setBookingId(original.getBookingId());
        return copy;
    }

    // -------------------------------------------------------------------------
    // Inner record for hold context
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of a user's active hold.
     * Stored in userActiveHolds map.
     *
     * NOTE: Java records expose accessors without the "get" prefix (e.g. seatIds()),
     * but JSTL EL requires JavaBean-style getters (getSeatIds()).
     * The methods below are aliases so that ${myHold.seatIds} works in JSPs.
     */
    public record HoldContext(String showId, List<String> seatIds, long expiresAt) {
        /** JavaBean alias for JSTL EL: ${myHold.showId} */
        public String getShowId()          { return showId; }
        /** JavaBean alias for JSTL EL: ${myHold.seatIds} */
        public List<String> getSeatIds()   { return seatIds; }
        /** JavaBean alias for JSTL EL: ${myHold.expiresAt} */
        public long getExpiresAt()         { return expiresAt; }
    }
}
