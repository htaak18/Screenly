<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<c:set var="pageTitle" value="Showtimes" scope="request"/>
<c:set var="navActive" value="movies"   scope="request"/>
<%@ include file="base.jspf" %>

<div class="container">
    <a href="${pageContext.request.contextPath}/user/movies" class="back-link">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
        Back to Movies
    </a>

    <%-- Movie hero: small poster + title + meta + description --%>
    <div class="movie-hero">
        <div class="movie-hero-poster">
            <c:choose>
                <c:when test="${not empty movie.posterUrl}">
                    <img src="${pageContext.request.contextPath}/<c:out value='${movie.posterUrl}'/>"
                         alt="<c:out value='${movie.title}'/> poster" loading="lazy"
                         onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">
                    <div class="movie-hero-poster-fallback" style="display:none">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                            <rect x="2" y="2" width="20" height="20" rx="2"/>
                            <line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/>
                            <line x1="2" y1="12" x2="22" y2="12"/>
                        </svg>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="movie-hero-poster-fallback">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                            <rect x="2" y="2" width="20" height="20" rx="2"/>
                            <line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/>
                            <line x1="2" y1="12" x2="22" y2="12"/>
                        </svg>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="movie-hero-info">
            <h1 class="movie-hero-title"><c:out value="${movie.title}"/></h1>
            <div class="movie-hero-meta">
                <span class="badge badge-red"><c:out value="${movie.genre}"/></span>
                <span class="badge"><c:out value="${movie.language}"/></span>
                <span class="badge"><c:out value="${movie.durationMinutes}"/> min</span>
                <span class="badge"><c:out value="${movie.rating}"/></span>
            </div>
            <p class="movie-hero-desc"><c:out value="${movie.description}"/></p>
        </div>
    </div>

    <p class="section-heading">Available Shows</p>

    <c:choose>
        <c:when test="${empty shows}">
            <div class="empty-state">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <p>No shows scheduled for this movie yet.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="shows-grid">
                <c:forEach var="show" items="${shows}">
                    <div class="show-card">
                        <div class="show-hall">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                <rect x="3" y="3" width="18" height="18" rx="2"/>
                                <polyline points="3 9 12 2 21 9"/>
                            </svg>
                            <c:out value="${show.hallName}"/>
                        </div>
                        <div class="show-datetime">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                            </svg>
                            <c:catch>
                                <fmt:parseDate value="${show.showDateTime}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pd" type="both"/>
                                <fmt:formatDate value="${pd}" pattern="d MMM yyyy, h:mm a"/>
                            </c:catch>
                            <c:if test="${empty pd}"><c:out value="${show.showDateTime}"/></c:if>
                        </div>
                        <div class="show-price-row">
                            <span class="show-price">
                                &#8377;<fmt:formatNumber value="${show.pricePerSeat}" type="number" maxFractionDigits="0" groupingUsed="false"/>
                            </span>
                            <span class="show-price-label">per seat</span>
                        </div>
                        <a href="${pageContext.request.contextPath}/user/seats?showId=<c:out value='${show.id}'/>"
                           class="btn btn-primary" style="justify-content:center;">
                            Select Seats
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                                <path d="M5 12h14M12 5l7 7-7 7"/>
                            </svg>
                        </a>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body></html>
