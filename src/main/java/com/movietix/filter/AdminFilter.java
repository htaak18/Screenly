package com.movietix.filter;

import com.movietix.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AdminFilter – protects all admin pages.
 *
 * Any request to /admin/* must come from a user with role "ADMIN".
 * Non-admin users are redirected to their dashboard with an error.
 * Unauthenticated users are redirected to /login.
 */
@WebFilter("/admin/*")
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session  = req.getSession(false);
        User        loggedIn = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        if (loggedIn == null) {
            // Not authenticated at all
            req.getSession().setAttribute("flashError", "Please log in to continue.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (!"ADMIN".equals(loggedIn.getRole())) {
            // Authenticated but not admin
            session.setAttribute("flashError", "Access denied: admin privileges required.");
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        // Admin – proceed
        chain.doFilter(request, response);
    }
}
