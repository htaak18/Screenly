/**
 * movies.js
 * Client-side search + genre-chip filtering for the movie grid.
 * No framework dependencies. Runs after DOM is ready (defer attribute).
 *
 * Reads:  #movieSearch, #genreChips, #movieGrid (.movie-card elements)
 * Writes: genre chip buttons into #genreChips, hides/shows .movie-card elements
 */
(function () {
    'use strict';

    var searchInput  = document.getElementById('movieSearch');
    var chipsWrapper = document.getElementById('genreChips');
    var grid         = document.getElementById('movieGrid');
    var noResults    = document.getElementById('noResults');
    var clearBtn     = document.getElementById('clearFiltersBtn');

    if (!grid) return; // movie grid not on this page

    /* ---- Collect cards ---- */
    var cards = Array.prototype.slice.call(grid.querySelectorAll('.movie-card'));

    /* ---- Build unique genre set from card data-genre attributes ---- */
    var genreSet = {};
    cards.forEach(function (card) {
        var raw = card.getAttribute('data-genre') || '';
        // genres may be "Sci-Fi / Thriller" – split on " / "
        raw.split(/\s*\/\s*/).forEach(function (g) {
            var trimmed = g.trim();
            if (trimmed) genreSet[trimmed] = true;
        });
    });

    /* ---- Render chips ---- */
    var activeGenre = ''; // '' means All

    function buildChips() {
        chipsWrapper.innerHTML = '';

        // "All" chip
        var allChip = createChip('All Movies', true);
        chipsWrapper.appendChild(allChip);

        Object.keys(genreSet).sort().forEach(function (genre) {
            chipsWrapper.appendChild(createChip(genre, false));
        });
    }

    function createChip(label, isActive) {
        var btn = document.createElement('button');
        btn.textContent = label;
        btn.className   = 'genre-chip' + (isActive ? ' active' : '');
        btn.type        = 'button';
        btn.setAttribute('aria-pressed', isActive ? 'true' : 'false');
        btn.addEventListener('click', function () {
            activeGenre = (label === 'All Movies') ? '' : label;
            // update active state
            Array.prototype.forEach.call(chipsWrapper.querySelectorAll('.genre-chip'), function (c) {
                var pressed = (c === btn);
                c.classList.toggle('active', pressed);
                c.setAttribute('aria-pressed', pressed ? 'true' : 'false');
            });
            filterCards();
        });
        return btn;
    }

    /* ---- Filter logic ---- */
    function filterCards() {
        var query = (searchInput ? searchInput.value : '').trim().toLowerCase();
        var shown = 0;

        cards.forEach(function (card) {
            var title = (card.getAttribute('data-title') || '').toLowerCase();
            var genre = (card.getAttribute('data-genre') || '');

            var matchesSearch = !query || title.indexOf(query) !== -1;
            var matchesGenre  = !activeGenre || genre.indexOf(activeGenre) !== -1;
            var visible       = matchesSearch && matchesGenre;

            card.style.display = visible ? '' : 'none';
            if (visible) shown++;
        });

        if (noResults) {
            noResults.style.display = (shown === 0) ? '' : 'none';
        }
    }

    /* ---- Search input listener ---- */
    if (searchInput) {
        searchInput.addEventListener('input', filterCards);
    }

    /* ---- Clear button ---- */
    if (clearBtn) {
        clearBtn.addEventListener('click', function () {
            if (searchInput) searchInput.value = '';
            activeGenre = '';
            Array.prototype.forEach.call(chipsWrapper.querySelectorAll('.genre-chip'), function (c) {
                var isAll = c.textContent === 'All Movies';
                c.classList.toggle('active', isAll);
                c.setAttribute('aria-pressed', isAll ? 'true' : 'false');
            });
            filterCards();
        });
    }

    /* ---- Init ---- */
    buildChips();
})();
