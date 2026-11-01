<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create Account" scope="request"/>
<%@ include file="base.jspf" %>

<div class="auth-wrapper">
    <div class="auth-card">
        <h2>Create Account</h2>
        <p class="subtitle">Join Screenly to book tickets online</p>

        <form method="post" action="${pageContext.request.contextPath}/register" id="registerForm" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">

            <div class="form-group">
                <label for="reg-username">Username</label>
                <input type="text" id="reg-username" name="username"
                       placeholder="3–20 letters, numbers, or underscore"
                       autocomplete="username" required
                       pattern="[A-Za-z0-9_]{3,20}" maxlength="20">
            </div>
            <div class="form-group">
                <label for="reg-email">Email</label>
                <input type="email" id="reg-email" name="email"
                       placeholder="you@example.com"
                       autocomplete="email" required>
            </div>
            <div class="form-group">
                <label for="reg-password">Password</label>
                <input type="password" id="reg-password" name="password"
                       placeholder="At least 6 characters"
                       autocomplete="new-password" required minlength="6">
            </div>
            <button type="submit" id="registerBtn" class="btn btn-primary" style="width:100%; justify-content:center; margin-top:0.5rem; padding:0.65rem;">
                Create Account
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
            </button>
        </form>

        <div class="auth-footer">
            Already have an account? <a href="${pageContext.request.contextPath}/login">Sign in</a>
        </div>
    </div>
</div>
</body></html>
