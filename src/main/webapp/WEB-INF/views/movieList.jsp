<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<c:set var="pageTitle"  value="Movies"  scope="request"/>
<c:set var="pageDesc"   value="Discover movies and book your tickets on Screenly." scope="request"/>
<c:set var="navActive"  value="movies"  scope="request"/>
<%@ include file="base.jspf" %>

<div class="container">
    <div class="page-header">
        <h1>Discover Movies</h1>
        <p>Find your next cinematic experience.</p>
    </div>

    <c:choose>
        <c:when test="${empty movies}">
            <div class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <rect x="2" y="2" width="20" height="20" rx="2"/>
                    <line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/>
                    <line x1="2" y1="12" x2="22" y2="12"/>
                </svg>
                <p>No movies available yet.</p>
                <c:if test="${sessionScope.loggedInUser.role == 'ADMIN'}">
                    <a href="${pageContext.request.contextPath}/admin/addMovie" class="btn btn-primary">Add Movie</a>
                </c:if>
            </div>
        </c:when>
        <c:otherwise>
            <%-- Search + genre filter (client-side via movies.js) --%>
            <div class="search-filter-bar">
                <input type="search" id="movieSearch" class="movie-search-input"
                       placeholder="Search movies..." aria-label="Search movies"
                       autocomplete="off">
            </div>
            <div class="genre-chips" id="genreChips" role="group" aria-label="Filter by genre">
                <%-- chips injected by movies.js --%>
            </div>

            <div class="movie-grid" id="movieGrid">
                <c:forEach var="movie" items="${movies}">
                    <a href="${pageContext.request.contextPath}/user/shows?movieId=<c:out value='${movie.id}'/>"
                       class="movie-card"
                       data-title="<c:out value='${movie.title}'/>"
                       data-genre="<c:out value='${movie.genre}'/>"
                       aria-label="<c:out value='${movie.title}'/>">

                        <%-- Poster: 2:3 aspect ratio --%>
                        <div class="movie-poster-wrap">
                            <c:choose>
                                <c:when test="${not empty movie.posterUrl}">
                                    <img src="${pageContext.request.contextPath}/<c:out value='${movie.posterUrl}'/>"
                                         alt="<c:out value='${movie.title}'/> poster"
                                         loading="lazy"
                                         onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">
                                    <div class="movie-poster-fallback" style="display:none">
                                        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                                            <rect x="2" y="2" width="20" height="20" rx="2"/>
                                            <line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/>
                                            <line x1="2" y1="12" x2="22" y2="12"/>
                                        </svg>
                                        <span><c:out value="${movie.title}"/></span>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="movie-poster-fallback">
                                        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                                            <rect x="2" y="2" width="20" height="20" rx="2"/>
                                            <line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/>
                                            <line x1="2" y1="12" x2="22" y2="12"/>
                                        </svg>
                                        <span><c:out value="${movie.title}"/></span>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <%-- Info --%>
                        <div class="movie-info">
                            <div class="movie-title"><c:out value="${movie.title}"/></div>
                            <div class="movie-meta">
                                <span class="badge badge-red"><c:out value="${movie.genre}"/></span>
                                <span class="badge"><c:out value="${movie.language}"/></span>
                                <span class="badge"><c:out value="${movie.durationMinutes}"/> min</span>
                                <span class="badge"><c:out value="${movie.rating}"/></span>
                            </div>
                            <div class="movie-card-footer">
                                <span class="movie-card-btn">
                                    View Showtimes
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                                        <path d="M5 12h14M12 5l7 7-7 7"/>
                                    </svg>
                                </span>
                            </div>
                        </div>
                    </a>
                </c:forEach>
            </div>

            <%-- No-results message (shown by JS) --%>
            <div class="empty-state no-results" id="noResults" aria-live="polite" style="display:none">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <p>No movies match your search.</p>
                <button class="btn btn-ghost" id="clearFiltersBtn">Clear filters</button>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<script src="${pageContext.request.contextPath}/static/js/movies.js" defer></script>
</body></html>
