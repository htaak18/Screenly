package com.movietix.model;

/**
 * Represents a single seat in a Show's seating grid.
 *
 * -----------------------------------------------------------------------
 * SEAT STATE MACHINE
 * -----------------------------------------------------------------------
 *
 *   AVAILABLE ──(hold)──► HELD ──(pay)──► BOOKED
 *                 │                           (terminal, persisted in XML)
 *                 └──(expire / cancel hold)──► AVAILABLE
 *
 * Key rules:
 *  - A seat is owned by exactly one state at any point in time.
 *  - Only the hold owner (holdOwnerUserId) may pay.
 *  - Holds expire after 7 minutes (holdExpiresAt). The server enforces this.
 *  - BOOKED seats can be cancelled by the booking owner; then the seat
 *    reverts to AVAILABLE.
 *
 * -----------------------------------------------------------------------
 * PERSISTENCE NOTES
 * -----------------------------------------------------------------------
 *  - BOOKED seats are persisted in seats.xml.
 *  - HELD seats are kept only in memory; on restart they revert to AVAILABLE.
 *  - AVAILABLE is the implicit default for any seat not in seats.xml.
 *
 * This class lives entirely in memory (managed by SeatService).
 * It is NOT directly serialised to XML; the SeatStore derives its XML
 * representation from the BOOKED seats only.
 */
public class Seat {

    /** All legal states a seat can be in. */
    public enum SeatState {
        AVAILABLE,
        HELD,
        BOOKED
    }

    private String    showId;           // which show this seat belongs to
    private String    seatId;           // e.g. "A1", "B4", "E8"
    private SeatState state;            // current state
    private String    holdOwnerUserId;  // non-null only when HELD
    private long      holdExpiresAt;    // epoch millis; 0 when not HELD
    private String    bookingId;        // non-null only when BOOKED

    public Seat() {}

    public Seat(String showId, String seatId) {
        this.showId = showId;
        this.seatId = seatId;
        this.state  = SeatState.AVAILABLE;
    }

    // ---- Getters & Setters ----

    public String    getShowId()               { return showId; }
    public void      setShowId(String showId)  { this.showId = showId; }

    public String    getSeatId()               { return seatId; }
    public void      setSeatId(String seatId)  { this.seatId = seatId; }

    public SeatState getState()                { return state; }
    public void      setState(SeatState state) { this.state = state; }

    public String    getHoldOwnerUserId()                          { return holdOwnerUserId; }
    public void      setHoldOwnerUserId(String holdOwnerUserId)    { this.holdOwnerUserId = holdOwnerUserId; }

    public long      getHoldExpiresAt()                      { return holdExpiresAt; }
    public void      setHoldExpiresAt(long holdExpiresAt)    { this.holdExpiresAt = holdExpiresAt; }

    public String    getBookingId()                    { return bookingId; }
    public void      setBookingId(String bookingId)   { this.bookingId = bookingId; }

    /**
     * Convenience: true if this seat's hold has expired relative to
     * the given current time (in epoch millis). Always false when not HELD.
     */
    public boolean isHoldExpired(long nowMillis) {
        return state == SeatState.HELD && nowMillis >= holdExpiresAt;
    }

    @Override
    public String toString() {
        return "Seat{show='" + showId + "', id='" + seatId + "', state=" + state + "}";
    }
}
