package com.movietix.servlet;

import com.movietix.model.Booking;
import com.movietix.model.Booking.BookingStatus;
import com.movietix.model.User;
import com.movietix.service.SeatService;
import com.movietix.store.BookingStore;
import com.movietix.listener.AppContextListener;
import com.movietix.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cancels a confirmed booking.
 *
 * POST /user/cancelBooking
 *   Params: bookingId, _csrf
 *
 * Rules enforced here:
 *  - Only the booking owner can cancel their own booking.
 *  - Only CONFIRMED bookings can be cancelled.
 *  - Only future shows can be cancelled (past shows cannot be refunded).
 *
 * On success: seats are freed, booking status → CANCELLED, mock refund message.
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/cancelBooking")
public class CancelBookingServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session  = req.getSession();
        User        loggedIn = (User) session.getAttribute("loggedInUser");

        // CSRF check
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        String bookingId = req.getParameter("bookingId");
        if (bookingId == null || bookingId.isBlank()) {
            session.setAttribute("flashError", "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        BookingStore bookingStore = (BookingStore) getServletContext().getAttribute(AppContextListener.BOOKING_STORE);
        Booking      booking      = bookingStore.findById(bookingId).orElse(null);

        if (booking == null) {
            session.setAttribute("flashError", "Booking not found.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        // Ownership check
        if (!loggedIn.getId().equals(booking.getUserId())) {
            session.setAttribute("flashError", "You can only cancel your own bookings.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        // Status check
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            session.setAttribute("flashError", "This booking is already cancelled.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        // Future show check
        try {
            LocalDateTime showTime = LocalDateTime.parse(booking.getShowDateTime(), DATE_FMT);
            if (showTime.isBefore(LocalDateTime.now())) {
                session.setAttribute("flashError", "Cannot cancel a booking for a past show.");
                resp.sendRedirect(req.getContextPath() + "/user/bookings");
                return;
            }
        } catch (Exception e) {
            // If date can't be parsed, allow cancellation
        }

        // All checks passed – cancel
        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);
        SeatService.OperationResult result = seatService.cancelBooking(booking);

        if (result.success()) {
            session.setAttribute("flashSuccess",
                "Booking " + bookingId + " cancelled. Mock refund of ₹" +
                String.format("%.2f", booking.getTotalPrice()) + " processed.");
        } else {
            session.setAttribute("flashError", result.message());
        }

        resp.sendRedirect(req.getContextPath() + "/user/bookings");
    }
}
