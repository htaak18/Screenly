package com.movietix.model;

/**
 * Represents a single show (screening) of a Movie.
 *
 * Stored in shows.xml.  Each Show links to a Movie by movieId.
 * The seat grid for a Show is 5 rows (A-E) × 8 seats = 40 seats total.
 */
public class Show {

    private String id;
    private String movieId;      // FK → Movie.id
    private String movieTitle;   // denormalised for display convenience
    private String showDateTime; // ISO-8601 string, e.g. "2024-12-25T18:30:00"
    private double pricePerSeat; // in rupees
    private String hallName;     // e.g. "Hall 1"

    public Show() {}

    public Show(String id, String movieId, String movieTitle,
                String showDateTime, double pricePerSeat, String hallName) {
        this.id           = id;
        this.movieId      = movieId;
        this.movieTitle   = movieTitle;
        this.showDateTime = showDateTime;
        this.pricePerSeat = pricePerSeat;
        this.hallName     = hallName;
    }

    // ---- Getters & Setters ----

    public String getId()              { return id; }
    public void   setId(String id)     { this.id = id; }

    public String getMovieId()                 { return movieId; }
    public void   setMovieId(String movieId)   { this.movieId = movieId; }

    public String getMovieTitle()                      { return movieTitle; }
    public void   setMovieTitle(String movieTitle)     { this.movieTitle = movieTitle; }

    public String getShowDateTime()                        { return showDateTime; }
    public void   setShowDateTime(String showDateTime)     { this.showDateTime = showDateTime; }

    public double getPricePerSeat()                      { return pricePerSeat; }
    public void   setPricePerSeat(double pricePerSeat)   { this.pricePerSeat = pricePerSeat; }

    public String getHallName()                  { return hallName; }
    public void   setHallName(String hallName)   { this.hallName = hallName; }

    @Override
    public String toString() {
        return "Show{id='" + id + "', movie='" + movieTitle + "', dateTime='" + showDateTime + "'}";
    }
}
