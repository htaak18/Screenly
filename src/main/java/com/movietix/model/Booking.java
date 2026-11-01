package com.movietix.model;

import java.util.List;

/**
 * Represents a confirmed (or cancelled) booking.
 *
 * Stored in bookings.xml. Created only after successful payment.
 *
 * Status transitions:
 *   CONFIRMED ──(user cancels future show)──► CANCELLED
 */
public class Booking {

    /** All legal statuses for a booking. */
    public enum BookingStatus {
        CONFIRMED,
        CANCELLED
    }

    private String        id;           // unique booking ID (e.g. "BK-00001")
    private String        userId;       // who made the booking
    private String        showId;
    private String        movieTitle;   // denormalised for display
    private String        showDateTime; // denormalised for display
    private List<String>  seatIds;      // e.g. ["A1","A2","B3"]
    private double        totalPrice;
    private BookingStatus status;
    private String        bookedAt;     // ISO-8601 string

    public Booking() {}

    public Booking(String id, String userId, String showId,
                   String movieTitle, String showDateTime,
                   List<String> seatIds, double totalPrice,
                   BookingStatus status, String bookedAt) {
        this.id           = id;
        this.userId       = userId;
        this.showId       = showId;
        this.movieTitle   = movieTitle;
        this.showDateTime = showDateTime;
        this.seatIds      = seatIds;
        this.totalPrice   = totalPrice;
        this.status       = status;
        this.bookedAt     = bookedAt;
    }

    // ---- Getters & Setters ----

    public String        getId()              { return id; }
    public void          setId(String id)     { this.id = id; }

    public String        getUserId()               { return userId; }
    public void          setUserId(String userId)  { this.userId = userId; }

    public String        getShowId()               { return showId; }
    public void          setShowId(String showId)  { this.showId = showId; }

    public String        getMovieTitle()                     { return movieTitle; }
    public void          setMovieTitle(String movieTitle)    { this.movieTitle = movieTitle; }

    public String        getShowDateTime()                       { return showDateTime; }
    public void          setShowDateTime(String showDateTime)    { this.showDateTime = showDateTime; }

    public List<String>  getSeatIds()                  { return seatIds; }
    public void          setSeatIds(List<String> seats) { this.seatIds = seats; }

    public double        getTotalPrice()                       { return totalPrice; }
    public void          setTotalPrice(double totalPrice)      { this.totalPrice = totalPrice; }

    public BookingStatus getStatus()                   { return status; }
    public void          setStatus(BookingStatus s)    { this.status = s; }

    public String        getBookedAt()                   { return bookedAt; }
    public void          setBookedAt(String bookedAt)    { this.bookedAt = bookedAt; }

    @Override
    public String toString() {
        return "Booking{id='" + id + "', user='" + userId + "', seats=" + seatIds + ", status=" + status + "}";
    }
}
