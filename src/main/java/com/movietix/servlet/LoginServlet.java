package com.movietix.servlet;

import com.movietix.model.User;
import com.movietix.service.AuthService;
import com.movietix.listener.AppContextListener;
import com.movietix.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles user login.
 *
 * GET  /login → show login form
 * POST /login → authenticate; on success store user in session, redirect
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // If already logged in, redirect to movies
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("loggedInUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/user/movies");
            return;
        }

        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession();

        // CSRF validation
        if (!CsrfUtil.isValid(session, req.getParameter("_csrf"))) {
            session.setAttribute("flashError", "Invalid form submission. Please try again.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        AuthService authService = (AuthService) getServletContext().getAttribute(AppContextListener.AUTH_SERVICE);
        AuthService.AuthResult result = authService.login(username, password);

        if (result.success()) {
            // Store user in session – the session is the "login state"
            session.setAttribute("loggedInUser", result.user());
            session.setMaxInactiveInterval(30 * 60); // 30-minute session timeout

            // Redirect based on role
            if ("ADMIN".equals(result.user().getRole())) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/user/movies");
            }
        } else {
            session.setAttribute("flashError", result.message());
            resp.sendRedirect(req.getContextPath() + "/login");
        }
    }
}
