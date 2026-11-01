<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 – Not Found | Screenly</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/screenly.css">
</head>
<body>
<div style="min-height:100vh; display:flex; align-items:center; justify-content:center; text-align:center; padding:2rem;">
    <div>
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5"
             style="margin:0 auto 1.5rem;" aria-hidden="true">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <h1 style="font-size:5rem; font-weight:800; color:var(--accent); margin:0; line-height:1;">404</h1>
        <h2 style="font-size:1.4rem; margin:0.5rem 0 0.75rem; font-weight:600;">Page Not Found</h2>
        <p style="color:var(--text-sec); margin-bottom:1.75rem; max-width:340px; margin-left:auto; margin-right:auto;">
            The page you're looking for doesn't exist or has been moved.
        </p>
        <a href="${pageContext.request.contextPath}/user/movies" class="btn btn-primary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M19 12H5M12 5l-7 7 7 7"/>
            </svg>
            Back to Movies
        </a>
    </div>
</div>
</body></html>
