package com.movietix.test;

import com.movietix.model.Seat;
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
 * TEST 4: HoldExpiryTest
 * =======================
 * Verifies that hold expiry is correctly enforced SERVER-SIDE:
 *
 *  1. User A holds seat B1.
 *  2. Clock advances by 7 minutes + 1 second (past expiry).
 *  3. Assert B1 is now AVAILABLE (lazy expiry works).
 *  4. User B can now successfully hold B1.
 *  5. User A's late payment attempt is REJECTED by the server.
 *
 * WHY a fake clock?
 *   We cannot wait 7 real minutes in a unit test. By injecting a
 *   MutableClock we can advance time by 7 minutes in a single line.
 *   This also proves that the server (not the browser) enforces expiry.
 *
 * WHY lazy expiry matters?
 *   If expiry was ONLY done by the scheduler (every 10 seconds), then
 *   between scheduler runs, an expired hold might block another user.
 *   Lazy expiry on every read/write call ensures correctness regardless
 *   of whether the scheduler has run recently.
 */
@DisplayName("TC-04: Hold Expiry – Server-Side Clock Enforcement")
class HoldExpiryTest {

    private SeatService seatService;
    private MutableClock clock;

    private static final String USER_A = "userA-expire";
    private static final String USER_B = "userB-newhold";
    private static final String SEAT   = "B1";

    @BeforeEach
    void setUp() throws Exception {
        clock       = new MutableClock(System.currentTimeMillis());
        seatService = TestHelper.createService(clock);
    }

    @Test
    @DisplayName("Expired hold frees the seat; other user can hold; original payer rejected")
    void hold_expires_and_seat_becomes_available() {
        // Step 1: User A holds seat B1
        OperationResult holdResult = seatService.holdSeats(
            TestHelper.SHOW_ID, List.of(SEAT), USER_A);
        assertTrue(holdResult.success(), "User A should hold B1 successfully");

        // Verify B1 is HELD
        SeatState stateAfterHold = findSeat(SEAT).getState();
        assertEquals(SeatState.HELD, stateAfterHold, "B1 should be HELD after hold");

        System.out.println("\n=== HoldExpiryTest ===");
        System.out.println("  Step 1: User A holds B1 ✅");

        // Step 2: Advance clock by 7 minutes + 1 second = 421,000 ms
        clock.advance(SeatService.HOLD_DURATION_MILLIS + 1000);
        System.out.println("  Step 2: Advanced clock by 7m 1s (past expiry)");

        // Step 3: Reading the seat (getSeatGrid) triggers lazy expiry → B1 = AVAILABLE
        SeatState stateAfterExpiry = findSeat(SEAT).getState();
        assertEquals(SeatState.AVAILABLE, stateAfterExpiry,
            "B1 should be AVAILABLE after hold expires (lazy expiry)");
        System.out.println("  Step 3: B1 is now AVAILABLE (lazy expiry) ✅");

        // Step 4: User B can now hold B1
        OperationResult bHold = seatService.holdSeats(
            TestHelper.SHOW_ID, List.of(SEAT), USER_B);
        assertTrue(bHold.success(), "User B should be able to hold B1 after A's hold expires");
        System.out.println("  Step 4: User B holds B1 ✅");

        // Step 5: User A tries to pay (expired hold) → must be REJECTED
        OperationResult aPayment = seatService.confirmPayment(
            TestHelper.SHOW_ID, USER_A, TestHelper.TEST_SHOW);
        assertFalse(aPayment.success(), "Expired hold: User A's payment must be rejected");
        System.out.println("  Step 5: User A's late payment REJECTED ✅ — " + aPayment.message());
        System.out.println("  → Server-side expiry CONFIRMED ✅");
    }

    /** Helper: gets the current state of a seat from the service. */
    private Seat findSeat(String seatId) {
        return seatService.getSeatGrid(TestHelper.SHOW_ID).stream()
            .filter(s -> seatId.equals(s.getSeatId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Seat not found: " + seatId));
    }
}
