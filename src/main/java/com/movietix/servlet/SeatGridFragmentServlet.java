package com.movietix.servlet;

import com.movietix.model.Seat;
import com.movietix.model.Seat.SeatState;
import com.movietix.model.User;
import com.movietix.service.SeatService;
import com.movietix.listener.AppContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Serves a JSON fragment of the current seat grid for a show.
 * Called every 5 seconds by the seat selection page's JavaScript polling.
 *
 * GET /user/seatGrid?showId=sh1
 *
 * Returns JSON array:
 * [{"seatId":"A1","state":"AVAILABLE","mine":false}, ...]
 *
 * "mine" = true if this seat is HELD by the currently logged-in user.
 * This allows the JS to colour "mine" seats yellow and "other" held seats orange.
 *
 * No HTML – just JSON. This is a lightweight polling endpoint.
 *
 * Protected by AuthFilter.
 */
@WebServlet("/user/seatGrid")
public class SeatGridFragmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String showId = req.getParameter("showId");
        User   user   = (User) req.getSession().getAttribute("loggedInUser");

        if (showId == null || showId.isBlank() || user == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        SeatService seatService = (SeatService) getServletContext().getAttribute(AppContextListener.SEAT_SERVICE);
        List<Seat>  seats       = seatService.getSeatGrid(showId);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // Prevent caching so JS always gets fresh data
        resp.setHeader("Cache-Control", "no-store");

        PrintWriter out = resp.getWriter();
        out.print("[");
        for (int i = 0; i < seats.size(); i++) {
            Seat s   = seats.get(i);
            boolean mine = s.getState() == SeatState.HELD
                           && user.getId().equals(s.getHoldOwnerUserId());
            out.print("{");
            out.print("\"seatId\":\"" + s.getSeatId() + "\",");
            out.print("\"state\":\""  + s.getState()  + "\",");
            out.print("\"mine\":"     + mine);
            out.print("}");
            if (i < seats.size() - 1) out.print(",");
        }
        out.print("]");
        out.flush();
    }
}
