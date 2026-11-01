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
import java.util.List;

/**
 * Displays the mock payment page and processes payment submissions.
 *
 * GET  /user/payment?showId=sh1 → show payment page with countdown timer
 * POST /user/payment             → process "Pay Now" or "Simulate Failure"
 *
 * KEY SECURITY RULE: The server re-validates the hold at payment time.
 * The browser timer is display-only.  A malicious user who submits the
 * payment form after the hold expired will be rejected here.
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/payment")
public class PaymentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session  = req.getSession();
        User        loggedIn = (User) session.getAttribute("loggedInUser");
        String      showId   = req.getParameter("showId");

        if (showId == null || showId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        // Verify the user actually has a hold
        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);
        SeatService.HoldContext hold = seatService.getActiveHold(loggedIn.getId());

        if (hold == null || !hold.showId().equals(showId)) {
            session.setAttribute("flashError", "No active hold. Please select seats first.");
            resp.sendRedirect(req.getContextPath() + "/user/seats?showId=" + showId);
            return;
        }

        ShowStore showStore = (ShowStore) getServletContext().getAttribute(AppContextListener.SHOW_STORE);
        Show      show      = showStore.findById(showId).orElse(null);

        if (show == null) {
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        double totalPrice = hold.seatIds().size() * show.getPricePerSeat();
        String csrfToken  = CsrfUtil.getOrCreateToken(session);

        req.setAttribute("show",         show);
        req.setAttribute("hold",         hold);
        req.setAttribute("totalPrice",   totalPrice);
        req.setAttribute("expiresAt",    hold.expiresAt());   // epoch millis for JS timer
        req.setAttribute("csrfToken",    csrfToken);

        req.getRequestDispatcher("/WEB-INF/views/payment.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session  = req.getSession();
        User        loggedIn = (User) session.getAttribute("loggedInUser");

        // CSRF check
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        String showId = req.getParameter("showId");
        String action = req.getParameter("action"); // "pay" or "fail"

        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);

        if ("fail".equals(action)) {
            // Simulate payment failure → release hold, seats go back to AVAILABLE
            seatService.releaseHold(loggedIn.getId());
            session.setAttribute("flashError", "Payment failed. Your seat hold has been released.");
            resp.sendRedirect(req.getContextPath() + "/user/seats?showId=" + showId);
            return;
        }

        // "pay" action: confirm payment
        ShowStore showStore = (ShowStore) getServletContext().getAttribute(AppContextListener.SHOW_STORE);
        Show show = showStore.findById(showId).orElse(null);

        if (show == null) {
            session.setAttribute("flashError", "Show not found.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        // Server-side re-validation of hold (expiry, ownership)
        SeatService.OperationResult result = seatService.confirmPayment(showId, loggedIn.getId(), show);

        if (result.success()) {
            session.setAttribute("flashSuccess", "Payment successful!");
            resp.sendRedirect(req.getContextPath() + "/user/confirmation?bookingId=" + result.bookingId());
        } else {
            session.setAttribute("flashError", result.message());
            resp.sendRedirect(req.getContextPath() + "/user/seats?showId=" + showId);
        }
    }
}
