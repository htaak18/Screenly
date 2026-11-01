package com.movietix.servlet;

import com.movietix.model.Movie;
import com.movietix.model.Show;
import com.movietix.store.BookingStore;
import com.movietix.store.MovieStore;
import com.movietix.store.ShowStore;
import com.movietix.listener.AppContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Admin dashboard – lists movies, shows, and recent bookings.
 *
 * GET /admin/dashboard
 *
 * Protected by AdminFilter (role must be ADMIN).
 */
@WebServlet("/admin/dashboard")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        MovieStore   movieStore   = (MovieStore)   getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        ShowStore    showStore    = (ShowStore)     getServletContext().getAttribute(AppContextListener.SHOW_STORE);
        BookingStore bookingStore = (BookingStore)  getServletContext().getAttribute(AppContextListener.BOOKING_STORE);

        List<Movie>   movies   = movieStore.findAll();
        List<Show>    shows    = showStore.findAll();

        req.setAttribute("movies",   movies);
        req.setAttribute("shows",    shows);
        req.setAttribute("allBookings", bookingStore.findAll());

        req.getRequestDispatcher("/WEB-INF/views/adminDashboard.jsp").forward(req, resp);
    }
}
