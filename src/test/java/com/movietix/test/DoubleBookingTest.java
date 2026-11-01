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
 * TEST 6: DoubleBookingTest
 * ==========================
 * Verifies that a BOOKED seat cannot be held or booked again by any user.
 *
 * Flow:
 *  1. User A holds seat D1 and pays (seat becomes BOOKED).
 *  2. User B tries to hold D1 → must FAIL.
 *  3. User A themselves try to hold D1 again → must also FAIL.
 *
 * WHY: Without this check, a refunded or cancelled seat might be double-booked.
 * The BOOKED state is final until explicitly cancelled.
 */
@DisplayName("TC-06: Double Booking Prevention")
class DoubleBookingTest {

    private SeatService seatService;
    private MutableClock clock;

    @BeforeEach
    void setUp() throws Exception {
        clock       = new MutableClock(System.currentTimeMillis());
        seatService = TestHelper.createService(clock);
    }

    @Test
    @DisplayName("A BOOKED seat cannot be held by any user")
    void booked_seat_cannot_be_held() {
        String userA = "userA-dbl";
        String userB = "userB-dbl";
        String seat  = "D1";

        // Step 1: User A holds and pays for D1
        OperationResult hold = seatService.holdSeats(TestHelper.SHOW_ID, List.of(seat), userA);
        assertTrue(hold.success(), "User A should hold D1");

        OperationResult pay = seatService.confirmPayment(
            TestHelper.SHOW_ID, userA, TestHelper.TEST_SHOW);
        assertTrue(pay.success(), "User A's payment should succeed");

        // Verify D1 is now BOOKED
        SeatState state = seatService.getSeatGrid(TestHelper.SHOW_ID).stream()
            .filter(s -> seat.equals(s.getSeatId()))
            .findFirst().get().getState();
        assertEquals(SeatState.BOOKED, state, "D1 should be BOOKED after payment");

        System.out.println("\n=== DoubleBookingTest Results ===");
        System.out.println("  Step 1: User A paid for D1 → D1 is BOOKED ✅");

        // Step 2: User B tries to hold D1 → FAIL
        OperationResult bHold = seatService.holdSeats(TestHelper.SHOW_ID, List.of(seat), userB);
        assertFalse(bHold.success(), "User B must NOT be able to hold a BOOKED seat");
        System.out.println("  Step 2: User B's hold attempt REJECTED ✅ — " + bHold.message());

        // Step 3: User A tries to hold D1 again → FAIL (already BOOKED)
        OperationResult aAgain = seatService.holdSeats(TestHelper.SHOW_ID, List.of(seat), userA);
        assertFalse(aAgain.success(), "User A must NOT be able to hold an already BOOKED seat");
        System.out.println("  Step 3: User A's re-hold attempt REJECTED ✅ — " + aAgain.message());
        System.out.println("  → Double booking PREVENTED ✅");
    }
}
