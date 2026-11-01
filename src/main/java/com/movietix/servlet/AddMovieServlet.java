package com.movietix.servlet;

import com.movietix.model.Movie;
import com.movietix.store.MovieStore;
import com.movietix.listener.AppContextListener;
import com.movietix.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;

/**
 * Admin: Add a new movie.
 *
 * GET  /admin/addMovie → show the add-movie form
 * POST /admin/addMovie → validate, save, redirect to dashboard
 *
 * Protected by AdminFilter.
 */
@WebServlet("/admin/addMovie")
public class AddMovieServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/views/addMovie.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession();

        // CSRF check
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/admin/addMovie");
            return;
        }

        String title       = req.getParameter("title");
        String genre       = req.getParameter("genre");
        String description = req.getParameter("description");
        String durationStr = req.getParameter("durationMinutes");
        String language    = req.getParameter("language");
        String rating      = req.getParameter("rating");
        String posterUrl   = req.getParameter("posterUrl");

        // Basic validation
        if (title == null || title.isBlank()) {
            session.setAttribute("flashError", "Movie title is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/addMovie");
            return;
        }

        int duration = 0;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "Duration must be a positive integer.");
            resp.sendRedirect(req.getContextPath() + "/admin/addMovie");
            return;
        }

        String id = "m-" + UUID.randomUUID().toString().substring(0, 8);
        Movie movie = new Movie(id, title, genre, description, duration, language, rating,
                                posterUrl == null ? "" : posterUrl);

        MovieStore movieStore = (MovieStore) getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        movieStore.save(movie);

        session.setAttribute("flashSuccess", "Movie '" + title + "' added successfully!");
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }
}
