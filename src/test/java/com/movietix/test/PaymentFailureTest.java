package com.movietix.test;

import com.movietix.model.Seat.SeatState;
import com.movietix.service.SeatService;
import com.movietix.service.SeatService.OperationResult;
import com.movietix.test.TestHelper.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TEST 5: PaymentFailureTest
 * ===========================
 * Simulates a payment failure and verifies that seats are returned
 * to AVAILABLE so other users can book them.
 *
 * Flow:
 *  1. User A holds seats C1, C2.
 *  2. Payment "fails" (releaseHold is called).
 *  3. Assert C1 and C2 are back to AVAILABLE.
 *  4. User B can now hold C1 and C2.
 */
@DisplayName("TC-05: Payment Failure – Seats Released")
class PaymentFailureTest {

    private SeatService seatService;

    @BeforeEach
    void setUp() throws Exception {
        seatService = TestHelper.createService(new MutableClock(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("Payment failure releases held seats back to AVAILABLE")
    void payment_failure_releases_seats() {
        String userA = "userA-payfail";
        String userB = "userB-payfail";
        List<String> seats = List.of("C1", "C2");

        // Step 1: User A holds C1, C2
        OperationResult hold = seatService.holdSeats(TestHelper.SHOW_ID, seats, userA);
        assertTrue(hold.success(), "User A should hold C1 and C2");

        // Verify they are HELD
        seatService.getSeatGrid(TestHelper.SHOW_ID).stream()
            .filter(s -> seats.contains(s.getSeatId()))
            .forEach(s -> assertEquals(SeatState.HELD, s.getState(),
                s.getSeatId() + " should be HELD"));

        // Step 2: Simulate payment failure → releaseHold
        OperationResult release = seatService.releaseHold(userA);
        assertTrue(release.success(), "Release should succeed");

        System.out.println("\n=== PaymentFailureTest Results ===");
        System.out.println("  User A held C1, C2 → payment failed → released");

        // Step 3: Verify C1, C2 are now AVAILABLE
        seatService.getSeatGrid(TestHelper.SHOW_ID).stream()
            .filter(s -> seats.contains(s.getSeatId()))
            .forEach(s -> {
                assertEquals(SeatState.AVAILABLE, s.getState(),
                    s.getSeatId() + " should be AVAILABLE after payment failure");
                System.out.println("  " + s.getSeatId() + " → AVAILABLE ✅");
            });

        // Step 4: User B can now hold the same seats
        OperationResult bHold = seatService.holdSeats(TestHelper.SHOW_ID, seats, userB);
        assertTrue(bHold.success(), "User B should be able to hold C1, C2 after A's failure");
        System.out.println("  User B can now hold C1, C2 ✅");
        System.out.println("  → Payment failure correctly releases seats ✅");
    }
}
