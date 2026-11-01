/**
 * seat-grid.js
 *
 * Polls the server every 5 seconds for updated seat states and recolours
 * the seat buttons accordingly.  This lets users see when someone else
 * holds or releases a seat in near-real-time.
 *
 * Endpoint: GET /user/seatGrid?showId=<id>
 * Response: JSON array of {seatId, state, mine}
 *
 * State → CSS class mapping:
 *   AVAILABLE            → seat-available  (dark + border)
 *   HELD  + mine=true   → seat-held-me    (red, held by current user)
 *   HELD  + mine=false  → seat-held-other (amber stripes, not clickable)
 *   BOOKED               → seat-booked    (muted grey stripes, not clickable)
 *
 * Summary bar elements (IDs):
 *   summarySeats  – text showing selected seat labels
 *   summaryTotal  – text showing total price (₹N)
 *   holdBtn       – submit button (enabled only when ≥1 seat selected)
 * Price per seat is read from: #pricePerSeatInput (integer rupees)
 */

(function () {
    'use strict';

    const showId        = document.getElementById('showIdInput')?.value;
    const contextPath   = document.getElementById('contextPathInput')?.value || '';
    const pricePerSeat  = parseInt(document.getElementById('pricePerSeatInput')?.value || '0', 10);

    if (!showId) return;

    // Set of seat IDs currently selected by the user (pre-hold)
    const selected = new Set();

    // ---- Seat click handler (toggle selection) ----
    document.querySelectorAll('.seat-available').forEach(btn => {
        btn.addEventListener('click', function () {
            const seatId = this.dataset.seatId;
            if (selected.has(seatId)) {
                selected.delete(seatId);
                this.classList.remove('selected');
            } else {
                if (selected.size >= 6) {
                    alert('You can select at most 6 seats.');
                    return;
                }
                selected.add(seatId);
                this.classList.add('selected');
            }
            updateHiddenInputs();
        });
    });

    /** Syncs the hidden seatIds[] inputs with the current selection set
     *  and updates the summary bar (seat labels + total price). */
    function updateHiddenInputs() {
        // Remove old hidden inputs
        document.querySelectorAll('input[name="seatIds"]').forEach(el => el.remove());

        const form = document.getElementById('holdForm');
        if (!form) return;

        selected.forEach(seatId => {
            const input  = document.createElement('input');
            input.type   = 'hidden';
            input.name   = 'seatIds';
            input.value  = seatId;
            form.appendChild(input);
        });

        // ---- Update Hold button ----
        const btn = document.getElementById('holdBtn');
        if (btn) {
            btn.textContent = selected.size > 0
                ? `Hold ${selected.size} Seat${selected.size > 1 ? 's' : ''}`
                : 'Hold Seats';
            btn.disabled = (selected.size === 0);
        }

        // ---- Update summary bar ----
        const sumSeats = document.getElementById('summarySeats');
        const sumTotal = document.getElementById('summaryTotal');

        if (sumSeats) {
            sumSeats.textContent = selected.size > 0
                ? Array.from(selected).sort().join(', ')
                : 'None';
        }
        if (sumTotal) {
            const total = selected.size * pricePerSeat;
            sumTotal.textContent = '\u20B9' + total.toLocaleString('en-IN');
        }
    }

    // ---- Polling: refresh seat states every 5 seconds ----
    function pollSeatGrid() {
        fetch(contextPath + '/user/seatGrid?showId=' + encodeURIComponent(showId), {
            credentials: 'same-origin',  // send session cookie
            headers: { 'Accept': 'application/json' }
        })
        .then(r => r.ok ? r.json() : Promise.reject(r.status))
        .then(seats => {
            seats.forEach(({ seatId, state, mine }) => {
                const btn = document.querySelector('[data-seat-id="' + seatId + '"]');
                if (!btn) return;

                // Determine new CSS class
                let newClass;
                if (state === 'AVAILABLE') {
                    newClass = 'seat-available';
                } else if (state === 'HELD' && mine) {
                    newClass = 'seat-held-me';
                } else if (state === 'HELD') {
                    newClass = 'seat-held-other';
                } else {
                    newClass = 'seat-booked'; // BOOKED
                }

                // Only update DOM if state has changed (avoid flicker)
                const stateClasses = ['seat-available','seat-held-me','seat-held-other','seat-booked','selected'];
                const hasChanged = !btn.classList.contains(newClass);
                if (hasChanged) {
                    stateClasses.forEach(c => btn.classList.remove(c));
                    btn.classList.add(newClass);

                    // If seat is no longer available, deselect it
                    if (state !== 'AVAILABLE' && selected.has(seatId)) {
                        selected.delete(seatId);
                        updateHiddenInputs();
                    }

                    // Update aria-label to reflect new state
                    const stateLabel = state === 'AVAILABLE' ? 'Available'
                        : (state === 'HELD' && mine) ? 'Held by you'
                        : (state === 'HELD')          ? 'Held by another user'
                        : 'Booked';
                    btn.setAttribute('aria-label', 'Seat ' + seatId + ' \u2013 ' + stateLabel);

                    // Update clickability
                    btn.disabled = (state !== 'AVAILABLE');

                    // Re-attach click listener if seat became available
                    if (state === 'AVAILABLE') {
                        btn.addEventListener('click', function onceClick() {
                            const id = this.dataset.seatId;
                            if (selected.has(id)) {
                                selected.delete(id);
                                this.classList.remove('selected');
                            } else {
                                if (selected.size >= 6) {
                                    alert('You can select at most 6 seats.');
                                    return;
                                }
                                selected.add(id);
                                this.classList.add('selected');
                            }
                            updateHiddenInputs();
                        });
                    }
                }
            });
        })
        .catch(err => console.warn('Seat grid poll error:', err));
    }

    // Poll immediately then every 5 seconds
    pollSeatGrid();
    setInterval(pollSeatGrid, 5000);

    // Initialise summary bar on load
    updateHiddenInputs();

})();
