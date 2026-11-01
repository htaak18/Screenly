package com.movietix.servlet;

import com.movietix.model.Seat;
import com.movietix.model.Show;
import com.movietix.model.User;
import com.movietix.service.SeatService;
import com.movietix.store.ShowStore;
import com.movietix.listener.AppContextListener;
import com.movietix.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Displays the interactive seat selection grid for a show.
 *
 * GET /user/seats?showId=sh1
 *
 * The page auto-refreshes the grid every 5 seconds via JavaScript polling
 * of the SeatGridFragmentServlet (/user/seatGrid?showId=sh1).
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/seats")
public class SeatSelectionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String showId = req.getParameter("showId");
        if (showId == null || showId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        ShowStore   showStore   = (ShowStore)   getServletContext().getAttribute(AppContextListener.SHOW_STORE);
        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);

        Show show = showStore.findById(showId).orElse(null);
        if (show == null) {
            req.getSession().setAttribute("flashError", "Show not found.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        User        loggedIn = (User) req.getSession().getAttribute("loggedInUser");
        List<Seat>  seats    = seatService.getSeatGrid(showId);
        SeatService.HoldContext myHold = seatService.getActiveHold(loggedIn.getId());

        // Provide CSRF token for the hold form
        String csrfToken = CsrfUtil.getOrCreateToken(req.getSession());

        req.setAttribute("show",      show);
        req.setAttribute("seats",     seats);
        req.setAttribute("myHold",    myHold);
        req.setAttribute("csrfToken", csrfToken);
        req.setAttribute("currentUserId", loggedIn.getId());

        req.getRequestDispatcher("/WEB-INF/views/seatSelection.jsp").forward(req, resp);
    }
}
