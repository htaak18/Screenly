<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign In" scope="request"/>
<%@ include file="base.jspf" %>

<div class="auth-wrapper">
    <div class="auth-card">
        <h2>Welcome back</h2>
        <p class="subtitle">Sign in to book your seats</p>

        <form method="post" action="${pageContext.request.contextPath}/login" id="loginForm" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">

            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username"
                       placeholder="Enter your username"
                       autocomplete="username" required maxlength="20">
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password"
                       placeholder="Enter your password"
                       autocomplete="current-password" required>
            </div>
            <button type="submit" id="loginBtn" class="btn btn-primary" style="width:100%; justify-content:center; margin-top:0.5rem; padding:0.65rem;">
                Sign In
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
            </button>
        </form>

        <div class="auth-footer">
            Don't have an account? <a href="${pageContext.request.contextPath}/register">Create one</a>
        </div>

        <%-- Demo credentials for viva --%>
        <div style="margin-top:1.25rem; padding:0.75rem; background:rgba(255,255,255,0.03);
                    border-radius:var(--radius-sm); border:1px solid var(--border);">
            <p style="font-size:0.7rem; color:var(--text-muted); text-align:center; margin-bottom:0.4rem; text-transform:uppercase; letter-spacing:0.06em;">
                Demo Credentials
            </p>
            <p style="font-size:0.78rem; color:var(--text-sec); text-align:center;">
                User: <strong style="color:var(--text)">alice / alice123</strong><br>
                Admin: <strong style="color:var(--text)">admin / admin123</strong>
            </p>
        </div>
    </div>
</div>
</body></html>
