/**
 * countdown.js
 *
 * Displays a live countdown on the payment page.
 * The target time (epoch millis) is passed from the server via a data attribute.
 *
 * WHY CLIENT-SIDE ONLY: The browser timer is purely cosmetic UX.
 * The real expiry enforcement is done SERVER-SIDE in SeatService.confirmPayment().
 * Even if this JS is disabled, the server will reject payment after hold expiry.
 */

(function () {
    'use strict';

    const el       = document.getElementById('countdown-display');
    if (!el) return;

    const expiresAt = parseInt(el.dataset.expiresAt, 10); // epoch millis from server

    function update() {
        const remaining = expiresAt - Date.now();

        if (remaining <= 0) {
            el.textContent = '0:00';
            el.classList.add('urgent');
            // Redirect to seat selection with a message
            const showId = el.dataset.showId;
            alert('Your seat hold has expired. Please select seats again.');
            window.location.href = el.dataset.expiredUrl || '/user/movies';
            return;
        }

        const minutes = Math.floor(remaining / 60000);
        const seconds = Math.floor((remaining % 60000) / 1000);
        el.textContent = minutes + ':' + String(seconds).padStart(2, '0');

        // Turn red/pulsing when < 60 seconds remain
        if (remaining < 60000) {
            el.classList.add('urgent');
        } else {
            el.classList.remove('urgent');
        }

        // Schedule next update in ~1 second
        setTimeout(update, 1000);
    }

    update();
})();
