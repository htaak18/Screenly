<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Admin Dashboard" scope="request"/>
<c:set var="navActive" value="admin"          scope="request"/>
<%@ include file="base.jspf" %>

<div class="container">
    <div class="page-header">
        <h1>Admin Dashboard</h1>
        <p>Manage movies, shows, and view all bookings.</p>
    </div>

    <%-- Quick actions --%>
    <div style="display:flex; gap:0.75rem; flex-wrap:wrap; margin-bottom:2rem;">
        <a href="${pageContext.request.contextPath}/admin/addMovie" class="btn btn-primary" id="addMovieBtn">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Movie
        </a>
        <a href="${pageContext.request.contextPath}/admin/addShow" class="btn btn-warning" id="addShowBtn">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            Add Show
        </a>
    </div>

    <%-- MOVIES --%>
    <div class="card" style="margin-bottom:1.5rem;">
        <p class="section-heading">
            Movies
            (<c:out value="${movies.size()}"/>)
        </p>
        <c:choose>
            <c:when test="${empty movies}">
                <p style="color:var(--text-muted); font-size:0.875rem;">No movies added yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th><th>Title</th><th>Genre</th>
                                <th>Language</th><th>Duration</th><th>Rating</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="m" items="${movies}">
                                <tr>
                                    <td style="color:var(--text-muted); font-size:0.78rem; font-family:monospace;">
                                        <c:out value="${m.id}"/>
                                    </td>
                                    <td><strong><c:out value="${m.title}"/></strong></td>
                                    <td><c:out value="${m.genre}"/></td>
                                    <td><c:out value="${m.language}"/></td>
                                    <td><c:out value="${m.durationMinutes}"/> min</td>
                                    <td><c:out value="${m.rating}"/></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- SHOWS --%>
    <div class="card" style="margin-bottom:1.5rem;">
        <p class="section-heading">
            Shows
            (<c:out value="${shows.size()}"/>)
        </p>
        <c:choose>
            <c:when test="${empty shows}">
                <p style="color:var(--text-muted); font-size:0.875rem;">No shows added yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th><th>Movie</th><th>Date &amp; Time</th>
                                <th>Hall</th><th>Price / Seat</th><th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${shows}">
                                <tr>
                                    <td style="color:var(--text-muted); font-size:0.78rem; font-family:monospace;">
                                        <c:out value="${s.id}"/>
                                    </td>
                                    <td><strong><c:out value="${s.movieTitle}"/></strong></td>
                                    <td style="font-size:0.83rem;">
                                        <c:catch>
                                            <fmt:parseDate value="${s.showDateTime}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pdS" type="both"/>
                                            <fmt:formatDate value="${pdS}" pattern="d MMM yyyy, h:mm a"/>
                                        </c:catch>
                                        <c:if test="${empty pdS}"><c:out value="${s.showDateTime}"/></c:if>
                                    </td>
                                    <td><c:out value="${s.hallName}"/></td>
                                    <td>&#8377;<fmt:formatNumber value="${s.pricePerSeat}" type="number" maxFractionDigits="0" groupingUsed="false"/></td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/user/seats?showId=<c:out value='${s.id}'/>"
                                           class="btn btn-ghost" style="font-size:0.78rem; padding:0.3rem 0.7rem;">
                                            View Seats
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- ALL BOOKINGS --%>
    <div class="card">
        <p class="section-heading">
            All Bookings
            (<c:out value="${allBookings.size()}"/>)
        </p>
        <c:choose>
            <c:when test="${empty allBookings}">
                <p style="color:var(--text-muted); font-size:0.875rem;">No bookings made yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>Booking ID</th><th>User</th><th>Movie</th>
                                <th>Show</th><th>Seats</th><th>Total</th><th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="b" items="${allBookings}">
                                <tr>
                                    <td style="font-family:monospace; font-size:0.8rem; color:var(--accent);">
                                        <c:out value="${b.id}"/>
                                    </td>
                                    <td style="font-size:0.83rem;"><c:out value="${b.userId}"/></td>
                                    <td><c:out value="${b.movieTitle}"/></td>
                                    <td style="font-size:0.8rem;">
                                        <c:catch>
                                            <c:set var="rawBAt" value="${fn:substring(b.showDateTime, 0, 19)}"/>
                                            <fmt:parseDate value="${rawBAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pdBA" type="both"/>
                                            <fmt:formatDate value="${pdBA}" pattern="d MMM yyyy, h:mm a"/>
                                        </c:catch>
                                        <c:if test="${empty pdBA}"><c:out value="${b.showDateTime}"/></c:if>
                                    </td>
                                    <td>
                                        <c:forEach var="sid" items="${b.seatIds}" varStatus="st">
                                            <c:out value="${sid}"/><c:if test="${!st.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                    <td>&#8377;<fmt:formatNumber value="${b.totalPrice}" type="number" maxFractionDigits="0" groupingUsed="false"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${b.status == 'CONFIRMED'}">
                                                <span class="badge badge-green">Confirmed</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge" style="color:var(--text-muted);">Cancelled</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body></html>
