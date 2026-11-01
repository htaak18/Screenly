package com.movietix.filter;

import com.movietix.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AuthFilter – protects all user pages from unauthenticated access.
 *
 * Any request to /user/* that does not have a logged-in user in the session
 * is redirected to /login with an appropriate message.
 *
 * This filter does NOT require any instance fields (stateless), so it is
 * safe to be instantiated once and called by many threads simultaneously.
 */
@WebFilter("/user/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false); // false: do NOT create a new session
        User loggedIn = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        if (loggedIn == null) {
            // Not logged in → redirect to login page with a flash message
            req.getSession().setAttribute("flashError", "Please log in to continue.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // User is authenticated – proceed
        chain.doFilter(request, response);
    }
}
