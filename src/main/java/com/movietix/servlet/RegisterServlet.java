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
 * Handles user registration.
 *
 * GET  /register → show the registration form
 * POST /register → validate input, create user, redirect to login
 *
 * IMPORTANT: Servlet instance fields must NOT hold per-request state.
 * All data flows through method parameters or the HttpServletRequest.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Put a CSRF token in the session for the form
        CsrfUtil.getOrCreateToken(req.getSession());
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession();

        // CSRF validation
        String submittedToken = req.getParameter("_csrf");
        if (!CsrfUtil.isValid(session, submittedToken)) {
            session.setAttribute("flashError", "Invalid form submission (CSRF token mismatch). Please try again.");
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }

        String username = req.getParameter("username");
        String email    = req.getParameter("email");
        String password = req.getParameter("password");

        AuthService authService = (AuthService) getServletContext().getAttribute(AppContextListener.AUTH_SERVICE);
        AuthService.AuthResult result = authService.register(username, email, password);

        if (result.success()) {
            session.setAttribute("flashSuccess", "Registration successful! Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            session.setAttribute("flashError", result.message());
            resp.sendRedirect(req.getContextPath() + "/register");
        }
    }
}
