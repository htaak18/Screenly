package com.movietix.servlet;

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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the "Hold Seats" form submission.
 *
 * POST /user/holdSeats
 *   Params: showId, seatIds[] (multi-value), _csrf
 *
 * On success: redirects to payment page with hold info in session.
 * On failure: redirects back to seat selection with an error message.
 *
 * The seat selection form submits via POST (not GET) so seat IDs are not
 * visible in the URL (though security here is enforced server-side by the hold).
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/holdSeats")
public class HoldSeatsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session  = req.getSession();
        User        loggedIn = (User) session.getAttribute("loggedInUser");

        // CSRF check
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission. Please try again.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        String   showId   = req.getParameter("showId");
        String[] seatArr  = req.getParameterValues("seatIds");

        if (showId == null || showId.isBlank() || seatArr == null || seatArr.length == 0) {
            session.setAttribute("flashError", "Please select at least one seat.");
            resp.sendRedirect(req.getContextPath() + "/user/seats?showId=" + showId);
            return;
        }

        List<String> seatIds = Arrays.asList(seatArr);

        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);
        SeatService.OperationResult result = seatService.holdSeats(showId, seatIds, loggedIn.getId());

        if (result.success()) {
            // Store hold info in session so PaymentServlet can retrieve it
            session.setAttribute("holdShowId", showId);
            session.setAttribute("holdSeatIds", seatIds);
            session.setAttribute("flashSuccess", result.message());
            resp.sendRedirect(req.getContextPath() + "/user/payment?showId=" + showId);
        } else {
            session.setAttribute("flashError", result.message());
            resp.sendRedirect(req.getContextPath() + "/user/seats?showId=" + showId);
        }
    }
}
