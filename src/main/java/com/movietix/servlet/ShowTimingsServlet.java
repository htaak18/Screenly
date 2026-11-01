package com.movietix.servlet;

import com.movietix.model.Movie;
import com.movietix.model.Show;
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
 * Shows available screening times for a given movie.
 *
 * GET /user/shows?movieId=m1
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/shows")
public class ShowTimingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String movieId = req.getParameter("movieId");
        if (movieId == null || movieId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        MovieStore movieStore = (MovieStore) getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        ShowStore  showStore  = (ShowStore)  getServletContext().getAttribute(AppContextListener.SHOW_STORE);

        Movie      movie = movieStore.findById(movieId).orElse(null);
        List<Show> shows = showStore.findByMovieId(movieId);

        if (movie == null) {
            req.getSession().setAttribute("flashError", "Movie not found.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        req.setAttribute("movie", movie);
        req.setAttribute("shows", shows);
        req.getRequestDispatcher("/WEB-INF/views/showTimings.jsp").forward(req, resp);
    }
}
