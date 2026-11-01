package com.movietix.servlet;

import com.movietix.model.Movie;
import com.movietix.store.MovieStore;
import com.movietix.listener.AppContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Shows the movie catalogue.
 *
 * GET /user/movies → list all available movies
 *
 * Protected by AuthFilter (requires login).
 */
@WebServlet("/user/movies")
public class MovieListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        MovieStore movieStore = (MovieStore) getServletContext().getAttribute(AppContextListener.MOVIE_STORE);
        List<Movie> movies = movieStore.findAll();

        req.setAttribute("movies", movies);
        req.getRequestDispatcher("/WEB-INF/views/movieList.jsp").forward(req, resp);
    }
}
