package com.movietix.servlet;

import com.movietix.model.Booking;
import com.movietix.model.User;
import com.movietix.store.BookingStore;
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
 * Shows all bookings for the logged-in user.
 *
 * GET /user/bookings
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/bookings")
public class MyBookingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User         loggedIn     = (User) req.getSession().getAttribute("loggedInUser");
        BookingStore bookingStore = (BookingStore) getServletContext().getAttribute(AppContextListener.BOOKING_STORE);

        List<Booking> bookings = bookingStore.findByUserId(loggedIn.getId());

        // Ensure CSRF token exists in session (cancel forms in JSP need it)
        CsrfUtil.getOrCreateToken(req.getSession());

        req.setAttribute("bookings", bookings);
        req.getRequestDispatcher("/WEB-INF/views/myBookings.jsp").forward(req, resp);
    }
}
