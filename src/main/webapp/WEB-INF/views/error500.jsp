<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 – Server Error | Screenly</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/screenly.css">
</head>
<body>
<div style="min-height:100vh; display:flex; align-items:center; justify-content:center; text-align:center; padding:2rem;">
    <div>
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="var(--amber)" stroke-width="1.5"
             style="margin:0 auto 1.5rem;" aria-hidden="true">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h1 style="font-size:5rem; font-weight:800; color:var(--amber); margin:0; line-height:1;">500</h1>
        <h2 style="font-size:1.4rem; margin:0.5rem 0 0.75rem; font-weight:600;">Internal Server Error</h2>
        <p style="color:var(--text-sec); margin-bottom:0.5rem; max-width:360px; margin-left:auto; margin-right:auto;">
            Something went wrong on our end. Please try again.
        </p>
        <c:if test="${not empty pageContext.exception}">
            <p style="font-size:0.78rem; color:var(--text-muted); margin-bottom:1.5rem; font-family:monospace;">
                <c:out value="${pageContext.exception.message}"/>
            </p>
        </c:if>
        <a href="${pageContext.request.contextPath}/user/movies" class="btn btn-primary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M19 12H5M12 5l-7 7 7 7"/>
            </svg>
            Back to Movies
        </a>
    </div>
</div>
</body></html>
