package com.movietix.servlet;

import com.movietix.model.Booking;
import com.movietix.store.BookingStore;
import com.movietix.listener.AppContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Shows the booking confirmation details after a successful payment.
 *
 * GET /user/confirmation?bookingId=BK-00001
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/confirmation")
public class BookingConfirmationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String bookingId = req.getParameter("bookingId");

        if (bookingId == null || bookingId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        BookingStore bookingStore = (BookingStore) getServletContext().getAttribute(AppContextListener.BOOKING_STORE);
        Booking booking = bookingStore.findById(bookingId).orElse(null);

        if (booking == null) {
            req.getSession().setAttribute("flashError", "Booking not found.");
            resp.sendRedirect(req.getContextPath() + "/user/bookings");
            return;
        }

        req.setAttribute("booking", booking);
        req.getRequestDispatcher("/WEB-INF/views/confirmation.jsp").forward(req, resp);
    }
}
