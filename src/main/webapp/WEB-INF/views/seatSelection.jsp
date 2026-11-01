<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<c:set var="pageTitle" value="Select Seats" scope="request"/>
<c:set var="navActive" value="movies"   scope="request"/>
<%@ include file="base.jspf" %>

<%-- Hidden inputs read by seat-grid.js — do NOT rename these IDs --%>
<input type="hidden" id="showIdInput"      value="<c:out value='${show.id}'/>">
<input type="hidden" id="contextPathInput" value="${pageContext.request.contextPath}">
<%-- Price per seat for the summary bar JS calculation --%>
<input type="hidden" id="pricePerSeatInput" value="<fmt:formatNumber value='${show.pricePerSeat}' type='number' maxFractionDigits='0' groupingUsed='false'/>">

<div class="container">
    <a href="${pageContext.request.contextPath}/user/shows?movieId=<c:out value='${show.movieId}'/>" class="back-link">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
        Back to Showtimes
    </a>

    <%-- Page header --%>
    <div class="seat-page-header">
        <h1><c:out value="${show.movieTitle}"/></h1>
        <div class="seat-page-meta">
            <span>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                </svg>
                <c:catch var="dtErr">
                    <fmt:parseDate value="${show.showDateTime}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pd" type="both"/>
                    <fmt:formatDate value="${pd}" pattern="d MMM yyyy, h:mm a"/>
                </c:catch>
                <c:if test="${not empty dtErr}"><c:out value="${show.showDateTime}"/></c:if>
            </span>
            <span>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <rect x="3" y="3" width="18" height="18" rx="2"/><polyline points="3 9 12 2 21 9"/>
                </svg>
                <c:out value="${show.hallName}"/>
            </span>
            <span>
                &#8377;<fmt:formatNumber value="${show.pricePerSeat}" type="number" maxFractionDigits="0" groupingUsed="false"/> per seat
            </span>
        </div>
    </div>

    <%-- Existing hold banner --%>
    <c:if test="${not empty myHold}">
        <div class="flash flash-success" style="margin-bottom:1rem;">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
            You currently hold seats <strong><c:out value="${myHold.seatIds}"/></strong> on this show.
            Selecting new seats will release this hold.
        </div>
    </c:if>

    <%-- SCREEN --%>
    <div class="screen-bar-wrap">
        <div class="screen-indicator"></div>
    </div>

    <%-- SEAT GRID (server-rendered; JS polling updates states every 5 s) --%>
    <div class="seat-grid-wrapper" id="seatGridWrapper">
        <c:set var="rows" value="A,B,C,D,E"/>
        <c:forTokens var="row" items="${rows}" delims=",">
            <div class="seat-row">
                <span class="seat-row-label" aria-hidden="true"><c:out value="${row}"/></span>
                <c:forEach begin="1" end="8" var="col">
                    <c:set var="seatId" value="${row}${col}"/>
                    <c:set var="foundSeat" value="${null}"/>
                    <c:forEach var="s" items="${seats}">
                        <c:if test="${s.seatId == seatId}">
                            <c:set var="foundSeat" value="${s}"/>
                        </c:if>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${empty foundSeat or foundSeat.state == 'AVAILABLE'}">
                            <button class="seat seat-available"
                                    data-seat-id="<c:out value='${seatId}'/>"
                                    aria-label="Seat <c:out value='${seatId}'/> – Available"
                                    type="button">
                                <c:out value="${seatId}"/>
                            </button>
                        </c:when>
                        <c:when test="${foundSeat.state == 'HELD' and foundSeat.holdOwnerUserId == currentUserId}">
                            <button class="seat seat-held-me" disabled
                                    data-seat-id="<c:out value='${seatId}'/>"
                                    aria-label="Seat <c:out value='${seatId}'/> – Held by you">
                                <c:out value="${seatId}"/>
                            </button>
                        </c:when>
                        <c:when test="${foundSeat.state == 'HELD'}">
                            <button class="seat seat-held-other" disabled
                                    data-seat-id="<c:out value='${seatId}'/>"
                                    aria-label="Seat <c:out value='${seatId}'/> – Held by another user">
                                <c:out value="${seatId}"/>
                            </button>
                        </c:when>
                        <c:otherwise>
                            <button class="seat seat-booked" disabled
                                    data-seat-id="<c:out value='${seatId}'/>"
                                    aria-label="Seat <c:out value='${seatId}'/> – Booked">
                                <c:out value="${seatId}"/>
                            </button>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>
        </c:forTokens>
    </div>

    <%-- Legend --%>
    <div class="seat-legend" aria-label="Seat status legend">
        <div class="legend-item">
            <div class="legend-swatch" style="background:#1C1C23;border-color:#3F3F46;" aria-hidden="true"></div>
            Available
        </div>
        <div class="legend-item">
            <div class="legend-swatch" style="background:#EF4444;border-color:#EF4444;" aria-hidden="true"></div>
            Selected / Your Hold
        </div>
        <div class="legend-item">
            <div class="legend-swatch" style="background:rgba(245,158,11,0.15);border-color:#F59E0B;" aria-hidden="true"></div>
            Held by Other
        </div>
        <div class="legend-item">
            <div class="legend-swatch" style="background:#27272A;border-color:#3F3F46;" aria-hidden="true"></div>
            Booked
        </div>
    </div>
</div>

<%-- Sticky summary / hold bar --%>
<div class="seat-summary-bar" id="summaryStickyBar">
    <form method="post"
          action="${pageContext.request.contextPath}/user/holdSeats"
          id="holdForm"
          style="display:contents;">
        <input type="hidden" name="_csrf"   value="<c:out value='${csrfToken}'/>">
        <input type="hidden" name="showId"  value="<c:out value='${show.id}'/>">
        <%-- seatIds[] hidden inputs are injected by seat-grid.js --%>

        <div class="seat-summary-info">
            <span class="seat-summary-label">Selected</span>
            <span class="seat-summary-value" id="summarySeats">None</span>
        </div>
        <div class="seat-summary-info">
            <span class="seat-summary-label">Total</span>
            <span class="seat-summary-value" id="summaryTotal">&#8377;0</span>
        </div>
        <div>
            <button type="submit" id="holdBtn" class="btn btn-primary" disabled>
                Hold Seats
            </button>
            <div class="seat-summary-hint" style="margin-top:4px;">Up to 6 seats. Held for 7 minutes.</div>
        </div>
    </form>
</div>

<script src="${pageContext.request.contextPath}/static/js/seat-grid.js" defer></script>
</body></html>
