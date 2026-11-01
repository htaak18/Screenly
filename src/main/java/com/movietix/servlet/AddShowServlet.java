package com.movietix.servlet;

import com.movietix.model.Movie;
import com.movietix.model.Show;
import com.movietix.service.SeatService;
import com.movietix.store.MovieStore;
import com.movietix.store.ShowStore;
import com.movietix.listener.AppContextListener;
import com.movietix.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Admin: Add a new show (screening).
 *
 * GET  /admin/addShow → show the add-show form (populates movie dropdown)
 * POST /admin/addShow → validate, save show, initialise seat grid, redirect
 *
 * After saving, SeatService.initShow() must be called so the new show's
 * seat grid exists in memory (otherwise the seat selection page will fail).
 *
 * Protected by AdminFilter.
 */
@WebServlet("/admin/addShow")
public class AddShowServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        MovieStore   movieStore = (MovieStore) getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        List<Movie>  movies     = movieStore.findAll();

        CsrfUtil.getOrCreateToken(req.getSession());
        req.setAttribute("movies", movies);
        req.getRequestDispatcher("/WEB-INF/views/addShow.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession();

        // CSRF check
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/admin/addShow");
            return;
        }

        String movieId      = req.getParameter("movieId");
        String showDateTime = req.getParameter("showDateTime");
        String priceStr     = req.getParameter("pricePerSeat");
        String hallName     = req.getParameter("hallName");

        if (movieId == null || movieId.isBlank() ||
            showDateTime == null || showDateTime.isBlank()) {
            session.setAttribute("flashError", "Movie and show date/time are required.");
            resp.sendRedirect(req.getContextPath() + "/admin/addShow");
            return;
        }

        double price = 0;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "Price must be a positive number.");
            resp.sendRedirect(req.getContextPath() + "/admin/addShow");
            return;
        }

        MovieStore movieStore = (MovieStore) getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        Movie      movie      = movieStore.findById(movieId).orElse(null);

        if (movie == null) {
            session.setAttribute("flashError", "Selected movie not found.");
            resp.sendRedirect(req.getContextPath() + "/admin/addShow");
            return;
        }

        // Normalise datetime: HTML datetime-local gives "2024-12-25T18:30"
        // Store as ISO-8601 (append seconds if missing)
        String normDateTime = showDateTime.contains(":") && showDateTime.length() == 16
                              ? showDateTime + ":00"
                              : showDateTime;

        String showId = "sh-" + UUID.randomUUID().toString().substring(0, 8);
        Show   show   = new Show(showId, movieId, movie.getTitle(), normDateTime, price,
                                 hallName == null || hallName.isBlank() ? "Hall 1" : hallName);

        ShowStore showStore = (ShowStore) getServletContext().getAttribute(AppContextListener.SHOW_STORE);
        showStore.save(show);

        // IMPORTANT: initialise in-memory seat grid for the new show
        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);
        seatService.initShow(showId);

        session.setAttribute("flashSuccess",
            "Show added for '" + movie.getTitle() + "' on " + normDateTime);
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }
}
