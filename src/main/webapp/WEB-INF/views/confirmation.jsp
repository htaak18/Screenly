<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Booking Confirmed" scope="request"/>
<c:set var="navActive" value="bookings"         scope="request"/>
<%@ include file="base.jspf" %>

<div class="container" style="max-width:580px;">
    <div class="confirm-card">
        <div class="confirm-icon">
            <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--green)" stroke-width="1.5" aria-hidden="true">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
        </div>
        <h1 style="font-size:1.5rem; font-weight:700;">Booking Confirmed</h1>
        <p class="confirm-detail" style="margin-top:0.25rem;">Your tickets are booked. Enjoy the show!</p>
        <div class="confirm-id">Booking ID: <c:out value="${booking.id}"/></div>

        <div class="divider"></div>

        <div style="text-align:left;">
            <table class="summary-table">
                <tr>
                    <td>Movie</td>
                    <td><c:out value="${booking.movieTitle}"/></td>
                </tr>
                <tr>
                    <td>Date &amp; Time</td>
                    <td>
                        <c:catch var="dtErrC">
                            <c:set var="rawDtC" value="${fn:substring(booking.showDateTime, 0, 19)}"/>
                            <fmt:parseDate value="${rawDtC}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pdC" type="both"/>
                            <fmt:formatDate value="${pdC}" pattern="d MMM yyyy, h:mm a"/>
                        </c:catch>
                        <c:if test="${not empty dtErrC}"><c:out value="${booking.showDateTime}"/></c:if>
                    </td>
                </tr>
                <tr>
                    <td>Seats</td>
                    <td>
                        <c:forEach var="sid" items="${booking.seatIds}" varStatus="st">
                            <span class="badge"><c:out value="${sid}"/></span>
                        </c:forEach>
                    </td>
                </tr>
                <tr>
                    <td>Booked At</td>
                    <td style="font-size:0.82rem; color:var(--text-sec);">
                        <c:catch>
                            <c:set var="bat" value="${fn:substring(booking.bookedAt, 0, 19)}"/>
                            <fmt:parseDate value="${bat}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pdAt" type="both"/>
                            <fmt:formatDate value="${pdAt}" pattern="d MMM yyyy, h:mm a"/>
                        </c:catch>
                        <c:if test="${empty pdAt}"><c:out value="${booking.bookedAt}"/></c:if>
                    </td>
                </tr>
                <tr class="total-row">
                    <td>Total Paid</td>
                    <td>&#8377;<fmt:formatNumber value="${booking.totalPrice}" type="number" maxFractionDigits="0" groupingUsed="false"/></td>
                </tr>
            </table>
        </div>

        <div class="divider"></div>

        <div style="display:flex; gap:0.75rem; justify-content:center; flex-wrap:wrap;">
            <a href="${pageContext.request.contextPath}/user/bookings" class="btn btn-primary">My Bookings</a>
            <a href="${pageContext.request.contextPath}/user/movies"   class="btn btn-ghost">Browse Movies</a>
        </div>
    </div>
</div>
</body></html>
