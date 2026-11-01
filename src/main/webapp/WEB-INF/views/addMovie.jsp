<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Add Movie" scope="request"/>
<c:set var="navActive" value="admin"          scope="request"/>
<%@ include file="base.jspf" %>

<div class="container" style="max-width:640px;">
    <a href="${pageContext.request.contextPath}/admin/dashboard" class="back-link">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
        </svg>
        Back to Dashboard
    </a>

    <div class="page-header">
        <h1>Add New Movie</h1>
        <p>Fill in the details to add a movie to the catalogue.</p>
    </div>

    <div class="card">
        <form method="post" action="${pageContext.request.contextPath}/admin/addMovie"
              id="addMovieForm" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">

            <div class="form-group">
                <label for="title">Movie Title *</label>
                <input type="text" id="title" name="title"
                       placeholder="e.g. Inception" required maxlength="200">
            </div>

            <div class="row">
                <div class="col">
                    <div class="form-group">
                        <label for="genre">Genre *</label>
                        <input type="text" id="genre" name="genre"
                               placeholder="e.g. Sci-Fi / Thriller" required maxlength="50">
                    </div>
                </div>
                <div class="col">
                    <div class="form-group">
                        <label for="language">Language *</label>
                        <input type="text" id="language" name="language"
                               placeholder="e.g. English" required maxlength="30">
                    </div>
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <div class="form-group">
                        <label for="durationMinutes">Duration (minutes) *</label>
                        <input type="number" id="durationMinutes" name="durationMinutes"
                               placeholder="e.g. 148" min="1" max="600" required>
                    </div>
                </div>
                <div class="col">
                    <div class="form-group">
                        <label for="rating">Rating</label>
                        <select id="rating" name="rating">
                            <option value="U">U (Universal)</option>
                            <option value="U/A" selected>U/A (Parental Guidance)</option>
                            <option value="A">A (Adults Only)</option>
                        </select>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <label for="description">Description</label>
                <textarea id="description" name="description" rows="3"
                          placeholder="Brief synopsis of the movie..."
                          maxlength="1000"></textarea>
            </div>

            <div class="form-group">
                <label for="posterUrl">Poster Path (optional)</label>
                <input type="text" id="posterUrl" name="posterUrl"
                       placeholder="images/posters/movie.jpg">
                <span style="font-size:0.75rem; color:var(--text-muted); margin-top:0.3rem; display:block;">
                    Relative path inside the webapp, e.g. images/posters/inception.jpg
                </span>
            </div>

            <button type="submit" id="addMovieSubmitBtn" class="btn btn-primary" style="width:100%; justify-content:center; padding:0.65rem;">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                Add Movie
            </button>
        </form>
    </div>
</div>
</body></html>
