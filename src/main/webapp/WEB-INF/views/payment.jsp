<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<c:set var="pageTitle" value="Payment" scope="request"/>
<c:set var="navActive" value="movies"  scope="request"/>
<%@ include file="base.jspf" %>

<div class="container" style="max-width:760px;">
    <div class="page-header">
        <h1>Complete Your Booking</h1>
        <p>Your seats are reserved. Complete payment before the timer runs out.</p>
    </div>

    <div class="payment-layout">
        <%-- LEFT: countdown + payment buttons --%>
        <div>
            <%-- Countdown timer --%>
            <div class="card" style="text-align:center; margin-bottom:1rem; padding:1.5rem;">
                <p style="font-size:0.8rem; color:var(--text-sec); margin-bottom:0.5rem; text-transform:uppercase; letter-spacing:0.08em;">Time Remaining</p>
                <div id="countdown-display"
                     class="countdown"
                     data-expires-at="${expiresAt}"
                     data-show-id="<c:out value='${show.id}'/>"
                     data-expired-url="${pageContext.request.contextPath}/user/seats?showId=<c:out value='${show.id}'/>">
                    7:00
                </div>
            </div>

            <%-- Payment actions --%>
            <div class="card" style="margin-bottom:1rem;">
                <p class="section-heading">Mock Payment</p>

                <form method="post" action="${pageContext.request.contextPath}/user/payment" style="margin-bottom:0.75rem;">
                    <input type="hidden" name="_csrf"   value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="showId"  value="<c:out value='${show.id}'/>">
                    <input type="hidden" name="action"  value="pay">
                    <button type="submit" id="payNowBtn" class="btn btn-primary" style="width:100%; justify-content:center; padding:0.7rem;">
                        Pay &#8377;<fmt:formatNumber value="${totalPrice}" type="number" maxFractionDigits="0" groupingUsed="false"/> – Confirm Booking
                    </button>
                </form>

                <form method="post" action="${pageContext.request.contextPath}/user/payment" style="margin-bottom:0.5rem;">
                    <input type="hidden" name="_csrf"   value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="showId"  value="<c:out value='${show.id}'/>">
                    <input type="hidden" name="action"  value="fail">
                    <button type="submit" id="payFailBtn" class="btn btn-ghost" style="width:100%; justify-content:center;">
                        Simulate Payment Failure
                    </button>
                </form>

                <form method="post" action="${pageContext.request.contextPath}/user/payment">
                    <input type="hidden" name="_csrf"   value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="showId"  value="<c:out value='${show.id}'/>">
                    <input type="hidden" name="action"  value="fail">
                    <button type="submit" id="cancelHoldBtn" class="btn btn-ghost" style="width:100%; justify-content:center; font-size:0.8rem; opacity:0.7;">
                        Cancel Hold &amp; Go Back
                    </button>
                </form>
            </div>
        </div>

        <%-- RIGHT: booking summary --%>
        <div class="card">
            <p class="section-heading">Booking Summary</p>
            <table class="summary-table">
                <tr>
                    <td>Movie</td>
                    <td><c:out value="${show.movieTitle}"/></td>
                </tr>
                <tr>
                    <td>Date &amp; Time</td>
                    <td>
                        <c:catch>
                            <fmt:parseDate value="${show.showDateTime}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="pd2" type="both"/>
                            <fmt:formatDate value="${pd2}" pattern="d MMM yyyy, h:mm a"/>
                        </c:catch>
                        <c:if test="${empty pd2}"><c:out value="${show.showDateTime}"/></c:if>
                    </td>
                </tr>
                <tr>
                    <td>Hall</td>
                    <td><c:out value="${show.hallName}"/></td>
                </tr>
                <tr>
                    <td>Seats</td>
                    <td>
                        <c:forEach var="sid" items="${holdSeatIds}" varStatus="st">
                            <span class="badge"><c:out value="${sid}"/></span>
                        </c:forEach>
                    </td>
                </tr>
                <tr>
                    <td>Price / Seat</td>
                    <td>&#8377;<fmt:formatNumber value="${show.pricePerSeat}" type="number" maxFractionDigits="0" groupingUsed="false"/></td>
                </tr>
                <tr class="total-row">
                    <td>Total</td>
                    <td>&#8377;<fmt:formatNumber value="${totalPrice}" type="number" maxFractionDigits="0" groupingUsed="false"/></td>
                </tr>
            </table>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/countdown.js" defer></script>
</body></html>
