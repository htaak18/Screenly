<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="My Bookings" scope="request"/>
<c:set var="navActive" value="bookings" scope="request"/>
<%@ include file="base.jspf" %>

<div class="container">
    <div class="page-header">
        <h1>My Bookings</h1>
        <p>All your ticket bookings in one place.</p>
    </div>

    <c:choose>
        <c:when test="${empty bookings}">
            <div class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <path d="M20 12V22H4V12"/><path d="M22 7H2v5h20V7z"/>
                    <path d="M12 22V7"/><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/>
                    <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/>
                </svg>
                <p>You haven't booked any tickets yet.</p>
                <a href="${pageContext.request.contextPath}/user/movies" class="btn btn-primary">Browse Movies</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="booking-cards-list">
                <c:forEach var="b" items="${bookings}">
                    <div class="booking-card">
                        <div class="booking-card-info">
                            <div class="booking-card-title"><c:out value="${b.movieTitle}"/></div>

                            <div class="booking-card-meta">
                                <span>
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                                    </svg>
                                    <c:catch var="dtErr">
                                        <c:set var="rawDt" value="${fn:substring(b.showDateTime, 0, 19)}"/>
                                        <fmt:parseDate value="${rawDt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pdB" type="both"/>
                                        <fmt:formatDate value="${pdB}" pattern="d MMM yyyy, h:mm a"/>
                                    </c:catch>
                                    <c:if test="${not empty dtErr}"><c:out value="${b.showDateTime}"/></c:if>
                                </span>
                            </div>

                            <div class="booking-card-seats">
                                <c:forEach var="sid" items="${b.seatIds}">
                                    <span class="badge"><c:out value="${sid}"/></span>
                                </c:forEach>
                            </div>

                            <div class="booking-card-footer">
                                <div>
                                    <div class="booking-card-id">ID: <c:out value="${b.id}"/></div>
                                    <div class="booking-total">
                                        &#8377;<fmt:formatNumber value="${b.totalPrice}" type="number" maxFractionDigits="0" groupingUsed="false"/>
                                    </div>
                                </div>

                                <div class="booking-actions">
                                    <c:choose>
                                        <c:when test="${b.status == 'CONFIRMED'}">
                                            <span class="badge badge-green">Confirmed</span>
                                            <form method="post"
                                                  action="${pageContext.request.contextPath}/user/cancelBooking"
                                                  onsubmit="return confirm('Cancel booking ${b.id}? A mock refund will be issued.');">
                                                <input type="hidden" name="_csrf"      value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="bookingId"  value="<c:out value='${b.id}'/>">
                                                <button type="submit" class="btn btn-ghost"
                                                        style="font-size:0.78rem; padding:0.3rem 0.7rem; border-color:var(--accent); color:var(--accent);">
                                                    Cancel
                                                </button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge" style="color:var(--text-muted);">Cancelled</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body></html>
