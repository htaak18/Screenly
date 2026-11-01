<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Add Show" scope="request"/>
<c:set var="navActive" value="admin"         scope="request"/>
<%@ include file="base.jspf" %>

<div class="container" style="max-width:560px;">
    <a href="${pageContext.request.contextPath}/admin/dashboard" class="back-link">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
        Back to Dashboard
    </a>

    <div class="page-header">
        <h1>Add New Show</h1>
        <p>Schedule a screening for an existing movie.</p>
    </div>

    <div class="card">
        <form method="post" action="${pageContext.request.contextPath}/admin/addShow"
              id="addShowForm" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">

            <div class="form-group">
                <label for="movieId">Select Movie *</label>
                <select id="movieId" name="movieId" required>
                    <option value="">— Choose a Movie —</option>
                    <c:forEach var="m" items="${movies}">
                        <option value="<c:out value='${m.id}'/>">
                            <c:out value="${m.title}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="form-group">
                <label for="showDateTime">Show Date &amp; Time *</label>
                <input type="datetime-local" id="showDateTime" name="showDateTime" required>
            </div>

            <div class="row">
                <div class="col">
                    <div class="form-group">
                        <label for="pricePerSeat">Price per Seat (&#8377;) *</label>
                        <input type="number" id="pricePerSeat" name="pricePerSeat"
                               placeholder="e.g. 250" min="0" step="0.01" required>
                    </div>
                </div>
                <div class="col">
                    <div class="form-group">
                        <label for="hallName">Hall Name</label>
                        <input type="text" id="hallName" name="hallName"
                               placeholder="e.g. Hall 1" maxlength="50">
                    </div>
                </div>
            </div>

            <p style="font-size:0.78rem; color:var(--text-muted); margin-bottom:1.25rem;">
                A 5&times;8 seat grid (40 seats) will be automatically created for this show.
            </p>

            <button type="submit" id="addShowSubmitBtn" class="btn btn-primary" style="width:100%; justify-content:center; padding:0.65rem;">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                Add Show
            </button>
        </form>
    </div>
</div>
</body></html>
