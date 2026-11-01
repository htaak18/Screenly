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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TEST 3: MultiSeatAtomicityTest
 * ================================
 * User A tries to hold seats [A1, A2, A3] while
 * User B concurrently tries to hold seats [A3, A4, A5].
 * Seat A3 is common to both requests.
 *
 * Expected: EXACTLY ONE user wins all 3 of their seats.
 *           The other gets NONE (all-or-nothing atomicity).
 *
 * WHY: Without all-or-nothing:
 *   - A gets A1, A2 and B gets A4, A5, but A3 is contested
 *     → partial hold: A has 2 seats, B has 2 seats (no winner paid for 3)
 *
 * The per-show lock guarantees that the entire check+mark sequence for a
 * request runs atomically. The second request sees A3 as HELD and fails completely.
 */
@DisplayName("TC-03: Multi-Seat All-or-Nothing Atomicity")
class MultiSeatAtomicityTest {

    private SeatService seatService;

    @BeforeEach
    void setUp() throws Exception {
        seatService = TestHelper.createService(new MutableClock(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("One user gets all 3 seats; the other gets none (shared seat A3)")
    void one_wins_all_other_wins_none() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done      = new CountDownLatch(2);

        AtomicInteger aSuccess = new AtomicInteger(0);
        AtomicInteger bSuccess = new AtomicInteger(0);

        // User A: holds A1, A2, A3
        Thread threadA = new Thread(() -> {
            try {
                startGate.await();
                OperationResult r = seatService.holdSeats(
                    TestHelper.SHOW_ID, List.of("A1","A2","A3"), "userA");
                if (r.success()) aSuccess.incrementAndGet();
            } catch (Exception e) { /* record as failure */ }
            finally { done.countDown(); }
        }, "Thread-A");

        // User B: holds A3, A4, A5 (A3 overlaps with A)
        Thread threadB = new Thread(() -> {
            try {
                startGate.await();
                OperationResult r = seatService.holdSeats(
                    TestHelper.SHOW_ID, List.of("A3","A4","A5"), "userB");
                if (r.success()) bSuccess.incrementAndGet();
            } catch (Exception e) { /* record as failure */ }
            finally { done.countDown(); }
        }, "Thread-B");

        threadA.start();
        threadB.start();
        startGate.countDown(); // release both simultaneously
        done.await(10, TimeUnit.SECONDS);

        int totalWinners = aSuccess.get() + bSuccess.get();

        System.out.println("\n=== MultiSeatAtomicityTest Results ===");
        System.out.println("  User A success: " + aSuccess.get());
        System.out.println("  User B success: " + bSuccess.get());
        System.out.println("  Total winners: " + totalWinners + " (expected: exactly 1)");

        // Verify seat states
        List<Seat> seats = seatService.getSeatGrid(TestHelper.SHOW_ID);
        Map<String, SeatState> stateMap = new java.util.HashMap<>();
        seats.forEach(s -> stateMap.put(s.getSeatId(), s.getState()));

        if (aSuccess.get() == 1) {
            System.out.println("  Winner: User A (A1, A2, A3 are HELD; A4, A5 still AVAILABLE)");
            assertEquals(SeatState.HELD,      stateMap.get("A1"));
            assertEquals(SeatState.HELD,      stateMap.get("A2"));
            assertEquals(SeatState.HELD,      stateMap.get("A3"));
            assertEquals(SeatState.AVAILABLE, stateMap.get("A4"));
            assertEquals(SeatState.AVAILABLE, stateMap.get("A5"));
        } else {
            System.out.println("  Winner: User B (A3, A4, A5 are HELD; A1, A2 still AVAILABLE)");
            assertEquals(SeatState.AVAILABLE, stateMap.get("A1"));
            assertEquals(SeatState.AVAILABLE, stateMap.get("A2"));
            assertEquals(SeatState.HELD,      stateMap.get("A3"));
            assertEquals(SeatState.HELD,      stateMap.get("A4"));
            assertEquals(SeatState.HELD,      stateMap.get("A5"));
        }

        System.out.println("  → All-or-nothing atomicity CONFIRMED ✅");
        assertEquals(1, totalWinners, "Exactly 1 user should win (the other gets none)");
    }
}
