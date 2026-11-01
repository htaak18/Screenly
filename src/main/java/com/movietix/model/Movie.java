package com.movietix.model;

/**
 * Represents a movie in the catalogue.
 *
 * Stored in movies.xml. Each movie has one or more Shows.
 */
public class Movie {

    private String id;
    private String title;
    private String genre;
    private String description;
    private int    durationMinutes; // e.g. 148
    private String language;        // e.g. "English"
    private String rating;          // e.g. "U/A"
    private String posterUrl;       // relative path to poster image (optional)

    public Movie() {}

    public Movie(String id, String title, String genre, String description,
                 int durationMinutes, String language, String rating, String posterUrl) {
        this.id              = id;
        this.title           = title;
        this.genre           = genre;
        this.description     = description;
        this.durationMinutes = durationMinutes;
        this.language        = language;
        this.rating          = rating;
        this.posterUrl       = posterUrl;
    }

    // ---- Getters & Setters ----

    public String getId()              { return id; }
    public void   setId(String id)     { this.id = id; }

    public String getTitle()               { return title; }
    public void   setTitle(String title)   { this.title = title; }

    public String getGenre()               { return genre; }
    public void   setGenre(String genre)   { this.genre = genre; }

    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }

    public int  getDurationMinutes()                      { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes)   { this.durationMinutes = durationMinutes; }

    public String getLanguage()                  { return language; }
    public void   setLanguage(String language)   { this.language = language; }

    public String getRating()                { return rating; }
    public void   setRating(String rating)   { this.rating = rating; }

    public String getPosterUrl()                   { return posterUrl; }
    public void   setPosterUrl(String posterUrl)   { this.posterUrl = posterUrl; }

    @Override
    public String toString() {
        return "Movie{id='" + id + "', title='" + title + "'}";
    }
}
